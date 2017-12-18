package com.effectiveosgi.lib.osgi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@RunWith(MockitoJUnitRunner.class)
public class SingletonServiceTrackerTest {
	
	private final AtomicLong id = new AtomicLong(0);
	
	@Mock
	BundleContext context;
	
	@Mock
	ServiceTrackerCustomizer<String, String> customizer;
	
	@Before
	@SuppressWarnings("unchecked")
	public void setup() {
		when(customizer.addingService(any(ServiceReference.class))).thenAnswer(i -> { 
			return i.getArgumentAt(0, MockServiceRef.class).getService();
		});
	}

	@Test
	public void testCustomizerReceivesFirstService() {
		InOrder order = inOrder(customizer);
		SingletonServiceTracker<String,String> tracker = new SingletonServiceTracker<>(context, String.class, customizer);
		
		MockServiceRef<String> ref1 = createService(1, "one");
		MockServiceRef<String> ref2 = createService(2, "two");
		MockServiceRef<String> ref3 = createService(3, "three");
		MockServiceRef<String> ref5 = createService(5, "five"); // create out of order so that ranking is checked
		MockServiceRef<String> ref4 = createService(4, "four");
		
		tracker.addingService(ref1); // produces an add event
		order.verify(customizer).addingService(ref1);
		tracker.addingService(ref2); // already bound -> no event
		tracker.addingService(ref3); // already bound -> no event
		tracker.addingService(ref4); // already bound -> no event
		tracker.addingService(ref5); // already bound -> no event
		
		tracker.removedService(ref2, "one"); // not bound -> no event
		tracker.removedService(ref1, "one"); // bound -> add event "five" (highest ranked) THEN removed event "one"
		order.verify(customizer).addingService(ref5);
		order.verify(customizer).removedService(ref1, ref1.getService());
		
		tracker.removedService(ref4, "five"); // not bound -> no event
		tracker.removedService(ref5, "five"); // bound -> add event "three" (highest ranked) THEN remove event "five"
		order.verify(customizer).addingService(ref3);
		order.verify(customizer).removedService(ref5, ref5.getService());
		
		tracker.removedService(ref3, "three"); // bind -> remove event "three"
		order.verify(customizer).removedService(ref3, ref3.getService());

		order.verifyNoMoreInteractions();
	}

	private MockServiceRef<String> createService(int rank, String string) {
		long id = this.id.getAndIncrement();
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(Constants.SERVICE_ID, id);
		if (rank > 0)
			props.put(Constants.SERVICE_RANKING, rank);
		return new MockServiceRef<>(string, props);
	}

}
