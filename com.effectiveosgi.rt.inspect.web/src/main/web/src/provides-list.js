import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/hardware-icons.js';
import '@polymer/iron-icons/places-icons.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import './abbrev-span.js';

class ProvidesList extends PolymerElement {
	static get template() {
		return html`
		<style>
	      :host {
	        display: block;
	      }
	      .inline { vertical-align: middle; }
	      iron-icon.inline {
	        --iron-icon-width: 16px;
	        --iron-icon-height: 16px;
	      }
	      td.expand {
	        vertical-align: top;
	      }
	      table.inline {
	        display: inline;
	      }
 	      tr.properties:nth-child(odd) {
	        background-color: rgba(245, 245, 245, 0.5);
	      }
	      td.column-left {
	        vertical-align: top;
	        padding-right: 10px;
	        font-weight: bold;
	        white-space: nowrap;
	      }
	      td.column-right {
	      	vertical-align: top;
	      	max-width: 800px;
	      }
	      .maxwidth {
	        width: 100%;
	      }
	      .number { color: var(--paper-blue-600); }
	      .warn { color: var(--paper-red-600); }
		</style>
		<table class="maxwidth">
			<col width="20px">
			<tr>
				<td class="expand"><iron-icon class="inline" icon="[[expandIcon]]" on-click="invertExpanded"></iron-icon>
				<td hidden$="[[expanded]]">[[count]] [[label]]&hellip;</td>
				<td hidden$="[[!expanded]]">
					<table class="maxwidth">
						<tr class="properties">
							<th colspan="2" style="text-align: left">[[count]] [[label]]:</th>
						</tr>
						<template is="dom-repeat" items="[[processedProvides]]">
							<tr class="properties">
								<td class="column-left">
									<iron-icon class="inline" icon="[[item.icon]]"></iron-icon>&nbsp;<code>[[item.ns]]
								</code></td>
								<td class="column-right">
									<table>
										<tr>
											<td>
												<code>[[item.text]]</code>
												<code class="number">[[item.version]]</code>
												<code hidden$="[[item.used]]" class="warn">[UNUSED]</code>
											</td>
										</tr>
										<template is="dom-repeat" items="[[item.consumers]]">
											<tr>
												<td class="column-right">
													<iron-icon class="inline number" icon="vaadin:arrows-long-right" id="usedByIcon"></iron-icon>
													<paper-tooltip for="usedByIcon">Required By...</paper-tooltip>
													<code><a href\$="#[[item.id]]">[[[item.id]]]</a> [[item.bsn]]</code>
													<code class="number">[[item.version]]]</code>
												</td>
											</tr>
										</template>
									</table>
								</td>
							</tr>
						</template>
					</table>
				</td>
			</tr>
		</table>
`;
	}
    static get is() { return 'provides-list'; }
    static get properties() {
        return {
            provides: Array,
            
            processedProvides: {
                type: Array,
                computed: 'processProvides(provides)'
            },
            
            label: {
                type: String,
                value: "provided capabilities"
            },
            
            count: {
                type: Number,
                computed: 'computeRowCount(provides)'
            },
            
            expanded: {
                type: Boolean,
                value: false
            },
            
            expandIcon: {
                type: String,
                computed: 'computeExpandIcon(expanded)'
            }
        };
    }
    
    computeRowCount(provides) {
        return provides.length;
    }
    
    computeExpandIcon(expanded) {
        return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
    }

    invertExpanded() {
        this.expanded = !this.expanded;
    }
    
    processProvides(provides) {
        var processed = [];
        for (var i = 0; i < provides.length; i++) {
            var provide = provides[i];
            
            var out;
            switch (provide.ns) {
            case "osgi.identity":
                out = {
                    ns: "Identity",
                    icon: "vaadin:info-circle-o",
                    text: provide.attribs["osgi.identity"],
                    version: provide.attribs.version,
                };
                break;
            case "osgi.wiring.bundle":
                out = {
                    ns: "Bundle",
                    icon: "vaadin:cubes",
                    text: provide.attribs["osgi.wiring.bundle"],
                    version: provide.attribs["bundle-version"],
                };
                break;
            case "osgi.wiring.host":
                out = {
                    ns: "Fragment Host",
                    icon: "vaadin:cubes",
                    text: provide.attribs["osgi.wiring.host"],
                    version: provide.attribs["bundle-version"],
                };
                break;
            case "osgi.wiring.package":
                out = {
                    ns: "Package",
                    icon: "vaadin:cube",
                    text: provide.attribs["osgi.wiring.package"],
                    version: provide.attribs.version,
                };
                break;
            case "osgi.service":
                out = {
                    ns: "Service",
                    icon: "vaadin:play",
                    text: provide.attribs["objectClass"],
                };
                break;
            case "osgi.native":
                out = {
                    ns: "Native",
                    icon: "hardware:memory",
                    text: provide.attribs["osgi.native.osname"] + " " + provide.attribs["osgi.native.osversion"] + " on " + provide.attribs["osgi.native.processor"],
                };
                break;
            case "osgi.ee":
                out = {
                    ns: "EE",
                    icon: "vaadin:cog",
                    text: provide.attribs["osgi.ee"],
                };
                break
            case "osgi.extender":
                out = {
                    ns: "Extender",
                    icon: "hardware:device-hub",
                    text: provide.attribs["osgi.extender"],
                    version: provide.attribs.version,
                };
                break;
            case "osgi.contract":
                out = {
                    ns : "Contract",
                    icon: "vaadin:diploma",
                    text: provide.attribs["osgi.contract"],
                    version: provide.attribs.version
                };
                break;
            default:
                out = {
                    ns: provide.ns,
                    icon: "vaadin:cog",
                    text: provide.attribs[provide.ns],
                    version: provide.attribs.version,
                };
            }
            out.used = provide.consumers.length > 0;
            out.consumers = provide.consumers;
            processed.push(out);
        }

        return processed;
    }

}
window.customElements.define(ProvidesList.is, ProvidesList);
