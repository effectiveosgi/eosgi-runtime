package com.effectiveosgi.rt.config.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import com.effectiveosgi.rt.config.ConfigFileReader;
import com.effectiveosgi.rt.config.ParsedRecord;
import com.effectiveosgi.rt.config.RecordIdentity;

class HierarchicalConfigInstaller extends ServiceTracker<ConfigFileReader, ConfigFileReader> implements ArtifactInstaller {
	
	static final String PID = "com.effectiveosgi.rt.config";

	private static final String PROP_PREFIX = "_" + PID;
	static final String PROP_FILE_PATH = PROP_PREFIX + ".filePath";
	
	private final class RankedReaderService implements Comparable<RankedReaderService> {
		private final long serviceId;
		private final ConfigFileReader reader;
		private final int rank;
		private final List<Pattern> patterns;
		private RankedReaderService(ConfigFileReader reader, ServiceReference<ConfigFileReader> ref) {
			serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
			this.reader = reader;
			
			Object rankingObj = ref.getProperty(Constants.SERVICE_RANKING);
			rank = (rankingObj instanceof Integer) ? (Integer) rankingObj : 0;
			
			Object patternsObj = ref.getProperty(ConfigFileReader.PROP_FILE_PATTERN);
			final String[] patternStrs;
			if (patternsObj instanceof String) {
				patternStrs = new String[] { (String) patternsObj };
			} else if (patternsObj instanceof String[]) {
				patternStrs = (String[]) patternsObj;
			} else {
				if (log != null) log.log(LogService.LOG_WARNING, String.format("%s service with ID %s from bundle %s_%s has is missing the %s property or it is the wrong type. No files will match.",
						ConfigFileReader.class.getSimpleName(), serviceId, ref.getBundle().getSymbolicName(), ref.getBundle().getVersion(), ConfigFileReader.PROP_FILE_PATTERN));
				patternStrs = new String[0];
			}

			patterns = new ArrayList<>(patternStrs.length);
			for (String patternStr : patternStrs) {
				try {
					patterns.add(Pattern.compile(patternStr));
				} catch (PatternSyntaxException e) {
					if (log != null) log.log(LogService.LOG_WARNING, String.format("Pattern parsing error on %s property of %s service with ID %d from bundle %s_%s: '%s'",
							ConfigFileReader.PROP_FILE_PATTERN, ConfigFileReader.class.getSimpleName(), serviceId, ref.getBundle().getSymbolicName(), ref.getBundle().getVersion(), patternStr), e);
				}
			}
			if (patterns.isEmpty() && log != null) {
				log.log(LogService.LOG_WARNING, String.format("No valid file name patterns on %s service with ID %d from bundle %s_%s. No files will match.",
						ConfigFileReader.class.getSimpleName(), serviceId, ref.getBundle().getSymbolicName(), ref.getBundle().getVersion()));
			}
		}
		@Override
		public int compareTo(RankedReaderService other) {
			int result = this.rank - other.rank;
			if (result == 0) result = (int) (other.serviceId - this.serviceId);
			return result;
		}
		@Override
		public String toString() {
			return "RankedReaderService [serviceId=" + serviceId + ", rank=" + rank + ", patterns=" + patterns + "]";
		}
	}
	
	private final SortedSet<RankedReaderService> rankedReaders = new TreeSet<>();
	private final ReadWriteLock rankedReadersLock = new ReentrantReadWriteLock();

	private final LogService log;
	private final ConfigurationAdmin configAdmin;
	
	HierarchicalConfigInstaller(BundleContext context, ConfigurationAdmin configAdmin, LogService log) {
		super(context, ConfigFileReader.class, null);
		this.configAdmin = configAdmin;
		this.log = log;
	}
	
	@Override
	public ConfigFileReader addingService(ServiceReference<ConfigFileReader> reference) {
		ConfigFileReader reader = context.getService(reference);
		RankedReaderService rankedEntry = new RankedReaderService(reader, reference);
		rankedReadersLock.writeLock().lock();
		try {
			rankedReaders.add(rankedEntry);
		} finally {
			rankedReadersLock.writeLock().unlock();
		}
		return reader;
	}
	@Override
	public void removedService(ServiceReference<ConfigFileReader> reference, ConfigFileReader reader) {
		long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
		rankedReadersLock.writeLock().lock();
		try {
			for (Iterator<RankedReaderService> iter = rankedReaders.iterator(); iter.hasNext(); ) {
				RankedReaderService entry = iter.next();
				if (serviceId == entry.serviceId) {
					iter.remove();
				}
			}
		} finally {
			rankedReadersLock.writeLock().unlock();
		}
	}
	
	private ConfigFileReader findReader(String fileName) {
		rankedReadersLock.readLock().lock();
		try {
			for (RankedReaderService rankedReader : rankedReaders) {
				for (Pattern pattern : rankedReader.patterns) {
					if (pattern.matcher(fileName).matches())
						return rankedReader.reader;
				}
			}
			return null;
		} finally {
			rankedReadersLock.readLock().unlock();
		}
	}

	@Override
	public boolean canHandle(File artifact) {
		return findReader(artifact.getName()) != null;
	}

	@Override
	public void install(File artifact) throws Exception {
		log(LogService.LOG_INFO, "Installing artifact " + artifact.getAbsolutePath(), null);
		loadConfig(artifact);
	}


	@Override
	public void update(File artifact) throws Exception {
		log(LogService.LOG_INFO, "Updating artifact " + artifact.getAbsolutePath(), null);
		loadConfig(artifact);
	}

	@Override
	public void uninstall(File artifact) throws Exception {
		log(LogService.LOG_INFO, "Uninstalling artifact" + artifact.getAbsolutePath(), null);
		deleteConfigs(artifact);
	}
	
	private void loadConfig(File artifact) throws IOException, IllegalArgumentException {
		String fileName = artifact.getName();
		ConfigFileReader reader = findReader(fileName);
		if (reader == null)
			throw new IllegalArgumentException("No reader found for config file name " + fileName);

		// Load the existing configs in ConfigurationAdmin
		Map<RecordIdentity, Configuration> existingConfigs = loadExistingConfigs(artifact);

		// Read the configs from the file and heal ConfigurationAdmin
		Stream<ParsedRecord> loadedRecords = reader.load(artifact);
		loadedRecords.forEach(record -> {
			Configuration existingConfig = existingConfigs.remove(record.getId());

			try {
				if (existingConfig != null) {
					// Update existing config if it is not different
					if (!matches(existingConfig.getProperties(), record.getProperties())) {
						log(LogService.LOG_DEBUG, String.format("Updating record %s into configuration pid=%s, factoryPid=%s", record.getId(), existingConfig.getPid(), existingConfig.getFactoryPid()), null);
						existingConfig.update(buildConfigDict(artifact, record.getId().getId(), record.getProperties()));
					}
				} else {
					// Create a new configuration
					Configuration config;
					if (record.getId().getFactoryId() != null) {
						config = configAdmin.getFactoryConfiguration(record.getId().getFactoryId(), record.getId().getId(), "?");
					} else {
						config = configAdmin.getConfiguration(record.getId().getId(), "?");
					}
					log(LogService.LOG_DEBUG, String.format("Updating record %s into configuration pid=%s, factoryPid=%s", record.getId(), config.getPid(), config.getFactoryPid()), null);
					config.update(buildConfigDict(artifact, record.getId().getId(), record.getProperties()));
				}
				
			} catch (IOException e) {
				log(LogService.LOG_ERROR, "Failed to modify record: " + record.getId(), e);
			}
		});
		
		// Delete any configurations remaining in the map
		for (Entry<RecordIdentity, Configuration> entry : existingConfigs.entrySet()) {
			Configuration existingConfig = entry.getValue();
			log(LogService.LOG_DEBUG, String.format("Deleting configuration pid=%s, factoryPid=%s: no matching record %s", existingConfig.getPid(), existingConfig.getFactoryPid(), entry.getKey()), null);
			existingConfig.delete();
		}
	}
	
	private Dictionary<String, Object> buildConfigDict(File artifact, String id, Map<String, ? extends Object> map) {
		Dictionary<String,Object> dict = new Hashtable<>(map);
		dict.put(PROP_FILE_PATH, artifact.getAbsolutePath());
		return dict;
	}

	private Map<RecordIdentity, Configuration> loadExistingConfigs(File artifact) throws IOException {
		try {
			final Map<RecordIdentity, Configuration> map;
			Configuration[] configs = configAdmin.listConfigurations(String.format("(%s=%s)", PROP_FILE_PATH, artifact.getAbsolutePath()));
			if (configs != null) {
				map = new HashMap<>(configs.length);
				for (Configuration config : configs) {
					Dictionary<String,Object> dict = config.getProperties();
					String configPid = config.getPid();

					String factoryId;
					String id;
					int tildeIndex = configPid.indexOf('~');
					if (tildeIndex >= 0) {
						factoryId = configPid.substring(0, tildeIndex);
						id = configPid.substring(tildeIndex + 1);
					} else {
						factoryId = null;
						id = configPid;
					}
					RecordIdentity identity = new RecordIdentity(id, factoryId);
					map.put(identity, config);
				}
			} else {
				map = new HashMap<>(0);
			}
			return map;
		} catch (InvalidSyntaxException e) {
			throw new IOException("Could not load existing configurations due to invalid filter syntax", e);
		}
	}

	private void deleteConfigs(File artifact) {
		try {
			String filter = String.format("(%s=%s)", PROP_FILE_PATH, artifact.getAbsolutePath());
			Configuration[] configs = configAdmin.listConfigurations(filter);
			if (configs != null) for (Configuration config : configs) {
				try {
					config.delete();
				} catch (Exception e) {
					log(LogService.LOG_ERROR, String.format("Failed to delete config %s for file %s", printConfigId(config), artifact.getAbsoluteFile()), e);
				}
			}
		} catch (IOException | InvalidSyntaxException e) {
			log(LogService.LOG_ERROR, "Error listing configurations for file " + artifact.getAbsolutePath(), null);
		}
	}

	private String printConfigId(Configuration config) {
		return config.getFactoryPid() == null ? String.format("pid=%s", config.getPid()) : String.format("factoryPid=%s, pid=%s", config.getFactoryPid(), config.getPid());
	}

	private void log(int level, String message, Throwable exception) {
		if (log != null) log.log(level, message, exception);
	}

	private void deleteConfigNoException(Configuration config) {
		String pid = config.getPid();
		String factoryPid = config.getFactoryPid();
		try {
			config.delete();
		} catch (IOException e) {
			log(LogService.LOG_WARNING, String.format("Failed to delete configuration pid=%s factoryPid=%s", pid, factoryPid), null);
		}
	}

	private static boolean matches(Dictionary<String, ?> dict, Map<String, ?> record) {
		return filterInfraProperties(dict).equals(record);
	}

	private static Map<String, Object> filterInfraProperties(Dictionary<String, ?> dict) {
		Map<String,Object> result = new HashMap<>(dict.size());
		for (Enumeration<String> e = dict.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			if (key.startsWith(PROP_PREFIX) || key.equals(Constants.SERVICE_PID) || key.equals(ConfigurationAdmin.SERVICE_FACTORYPID))
				continue;
			result.put(key, dict.get(key));
		}
		return result;
	}

}
