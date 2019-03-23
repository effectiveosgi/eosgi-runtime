import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';

import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import './property-table.js';
import './provides-list.js';
import './requires-list.js';
/**
 * @customElement
 * @polymer
 */
class BundleCard extends PolymerElement {
  static get template() {
    return html`
    <style>
      :host {
        display: block;
      }
      
      paper-card {
        width: 100%;
        min-width: var(--bundle-card-min-width, 1000px);
        background-color: var(--bundle-card-background, white);
      }
      
      .header-bar {
        font-size: 16px;
        font-weight: 400;
        color: var(--paper-grey-800);
      }

      th, td {
        font-weight: inherit;
        text-align: left;
      }

      td.bundle-header-name {
        font-weight: bold;
      }

      .inline {
        vertical-align: middle;
      }

      iron-icon.inline {
        --iron-icon-width: 16px;
        --iron-icon-height: 16px;
      }

      .maxwidth {
        width: 100%;
      }
      
      .header-right-item {
        font-size: 24px;
        vertical-align: middle;
      }

      iron-icon.header-right-item {
        --iron-icon-width: 24px;
        --iron-icon-height: 24px;
      }

      .header-right {
        float: right;
      }

      .number { color: var(--paper-blue-600); }
      .light { color: var(--paper-grey-600); }
      .good { color: var(--paper-green-700); }
      .error { color: var(--paper-red-800); }
    </style>
    
    <paper-card>
        <div class="card-content">
            <table class="maxwidth">
                <colgroup><col width="30px">
                </colgroup><thead>
                    <tr class="header-bar">
                        <th><iron-icon icon="[[expandIcon]]" on-click="invertExpanded" class="light"></iron-icon>
                        </th><th>
                            <iron-icon icon="vaadin:cubes"></iron-icon>
                            [[[bundle.id]]]
                            <code>[[bundle.symbolicName]]</code>
                            <span class="number">[[bundle.version]]</span>
                            <div class="header-right">
                                <iron-icon id="stateIcon" icon="[[stateUI.icon]]" class\$="[[stateUI.clazz]]"></iron-icon>
                                <paper-tooltip for="stateIcon">[[stateUI.tooltip]]</paper-tooltip>
                            </div>
                        </th>
                    </tr>
                </thead>
                <tbody hidden\$="[[!expanded]]">
                    <tr><td></td>
                        <td>Last Modified at <span class="number">[[bundle.lastModifiedISO]]</span></td>
                    </tr>
                    <!-- Bundle Headers Table -->
                    <tr><td></td>
                        <td>
                            <property-table properties="[[bundle.headers]]" label="bundle headers"></property-table>
                        </td>
                    </tr>
                    <!-- Provided Capabilities -->
                    <tr><td></td>
                    	<td>
                    		<provides-list provides="[[bundle.wiring.provides]]" label="provided capabilities">
                    	</provides-list></td>
                    </tr>
                    <!-- Provided Capabilities -->
                    <tr><td></td>
                    	<td>
                    		<requires-list requires="[[bundle.wiring.requires]]" label="required capabilities"></requires-list>
                    	</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </paper-card>
`;
  }

  static get is() { return 'bundle-card'; }
  static get properties() {
      return {

          bundle: Object,
          
          stateUI: {
              type: Object,
              computed: 'computeStateUI(bundle)'
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
  computeStateUI(bundle) {
      var state = {};
      switch (bundle.state) {
      case 1: // UNINSTALLED
          state = { icon: "vaadin:trash", clazz: "error", tooltip: "UNINSTALLED" };
          break;
      case 2: // INSTALLED
          state = { icon: "vaadin:check-circle-o", clazz: "light", tooltip: "INSTALLED" };
          break;
      case 4: // RESOLVED
          state = { icon: "vaadin:check-circle", clazz: "good", tooltip: "RESOLVED" };
          break;
      case 8: // STARTING
          state = { icon: "vaadin:play", clazz: "light", tooltip: "STARTING" };
      case 16: // STOPPING
          state.tooltip = "STOPPING";
          break;
      case 32: // ACTIVE
          state = { icon: "vaadin:play", clazz: "good", tooltip: "ACTIVE" };
          break;
      default:
          state = { icon: "vaadin:question", clazz: "error", tooltip: "UNKNOWN" };
      }
      state.clazz = "header-right-item " + state.clazz;
      return state;
  }
  computeExpandIcon(expanded) {
      return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
  }

  invertExpanded() {
      this.expanded = !this.expanded;
  }
}
window.customElements.define(BundleCard.is, BundleCard);
