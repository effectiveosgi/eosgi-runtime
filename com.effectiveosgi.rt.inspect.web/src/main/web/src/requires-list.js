import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/hardware-icons.js';
import '@polymer/iron-icons/places-icons.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import './abbrev-span.js';
class RequiresList extends PolymerElement {
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
	      	vertical-align: center;
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
						<template is="dom-repeat" items="[[processedRequires]]">
							<tr class="properties">
								<td class="column-left">
									<iron-icon class="inline" icon="[[item.icon]]"></iron-icon>&nbsp;<code>[[item.ns]]</code>
								</td>
								<td class="column-right">
									<table>
										<tr>
											<td>
												<code style="overflow-wrap: break-word">[[item.text]]</code>
												<code class="number" hidden$="[[!item.resolution]]">[[item.resolution]]</code>
											</td>
										</tr>
										<tr hidden$="[[item.hasProvider]]">
											<td><code class="warn">[UNSATISFIED]</code></td>
										</tr>
										<tr hidden$="[[!item.hasProvider]]">
											<td>
												<iron-icon class="inline number" icon="vaadin:arrow-long-left" id="providedByIcon"></iron-icon>
												<paper-tooltip for="providedByIcon">Provided By...</paper-tooltip>
												<code><a href\$="#[[item.provider.id]]">[[[item.provider.id]]]</a> [[item.provider.bsn]]</code>
												<code class="number">[[item.provider.version]]</code>
											</td>
										</tr>
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
    static get is() { return 'requires-list'; }
    static get properties() {
				return {
            requires: Array,
            
            processedRequires: {
                type: Array,
                computed: 'processRequires(requires)'
            },
            
            label: {
                type: String,
                value: "required capabilities"
            },
            
            count: {
                type: Number,
                computed: 'computeRowCount(requires)'
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
    
    computeRowCount(requires) {
				return requires.length;
    }
    
    computeExpandIcon(expanded) {
        return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
    }

    invertExpanded() {
        this.expanded = !this.expanded;
    }
    
    processRequires(requires) {
				var processed = [];
				for (var i = 0; i < requires.length; i++) {
            var require = requires[i];
            
            var out;
            switch (require.ns) {
            case "osgi.identity":
                out = {
                    ns: "Identity",
                    icon: "vaadin:info-circle-o",
                    text: require.directives.filter,
                };
                break;
            case "osgi.wiring.bundle":
                out = {
                    ns: "Bundle",
                    icon: "vaadin:cubes",
                    text: require.directives.filter,
                };
                break;
            case "osgi.wiring.host":
                out = {
                    ns: "Fragment Host",
                    icon: "vaadin:cubes",
                    text: require.directives.filter,
                };
                break;
            case "osgi.wiring.package":
                out = {
                    ns: "Package",
                    icon: "vaadin:cube",
                    text: require.directives.filter,
                };
                break;
            case "osgi.service":
                out = {
                    ns: "Service",
                    icon: "vaadin:play",
                    text: require.directives.filter,
                };
                break;
            case "osgi.native":
                out = {
                    ns: "Native",
                    icon: "hardware:memory",
                    text: require.directives.filter,
                };
                break;
            case "osgi.ee":
                out = {
                    ns: "EE",
                    icon: "vaadin:cog",
                    text: require.directives.filter,
                };
                break;
            case "osgi.extender":
                out = {
                    ns: "Extender",
                    icon: "hardware:device-hub",
                    text: require.directives.filter,
                };
                break;
            case "osgi.contract":
                out = {
                    ns : "Contract",
                    icon: "vaadin:diploma",
                    text: require.directives.filter,
                };
                break;
            case "osgi.unresolvable":
                out = {
                    ns : "Unresolvable",
                    icon: "vaadin:ban",
                    text: require.directives.filter,
                };
                break;
            default:
                out = {
                    ns: require.ns,
                    icon: "vaadin:cog",
                    text: require.directives.filter,
                };
            }
            out.resolution = require.directives.resolution;
            out.hasProvider = typeof(require.provider) !== "undefined";
            out.provider = require.provider;
            processed.push(out);
            }
        return processed;
    }

}
window.customElements.define(RequiresList.is, RequiresList);
