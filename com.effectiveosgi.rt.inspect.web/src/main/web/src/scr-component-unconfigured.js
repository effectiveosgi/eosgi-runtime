import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/paper-card/paper-card.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import './property-table.js';
import './scr-component-description.js';
/**
 * @customElement
 * @polymer
 */
class ScrComponentUnconfiguredComponent extends PolymerElement {
  static get template() {
    return html`
        <style>
        :host {
            display: block;
        }
        
        paper-card {
          width: 100%;
          min-width: 800px;
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
        .maxwidth {
            width: 100%;
        }
        .header-right {
          float: right;
        }
        .light { color: var(--paper-grey-600); }
        </style>
        <paper-card>
            <div class="card-content">
                <table class="maxwidth">
                    <colgroup><col width="30px">
                    </colgroup><thead>
                        <tr class="header-bar">
                            <th><iron-icon icon="[[expandIcon]]" on-click="invertExpanded" class="light"></iron-icon></th>
                            <th>
                                [[componentDescription.name]]
                                <div class="header-right">
                                    <iron-icon id="enabledIcon" icon="[[defaultEnabledUI.icon]]" class="light"></iron-icon>
                                    <paper-tooltip for="enabledIcon">[[defaultEnabledUI.tooltip]]</paper-tooltip>
                                    
                                    <iron-icon id="stateIcon" icon="vaadin:pause" class="light"></iron-icon>
                                    <paper-tooltip for="stateIcon">Awaiting Configuration</paper-tooltip>
                                </div>
                            </th>
                        </tr>
                    </thead>
                    <tbody hidden\$="[[!expanded]]">
                        <tr><td></td>
                            <td><scr-component-description component-description="[[componentDescription]]"></scr-component-description></td>
                        </tr>
                        <tr><td></td>
                            <td><property-table properties="[[componentDescription.properties]]" label="component properties"></property-table></td>
                        </tr>
                    </tbody>
                </table>
                    
            </div>
        </paper-card>
`;
  }

  static get is() { return 'scr-component-unconfigured'; }
  static get properties() {
      return {
          componentDescription: Object,
          
          expanded: {
              type: Boolean,
              value: false
          },

          expandIcon: {
              type: String,
              computed: 'computeExpandIcon(expanded)'
          },
          
          defaultEnabledUI: {
              type: Object,
              computed: 'computeDefaultEnabledUI(componentDescription)'
          }
      };
  }
  computeExpandIcon(expanded) {
      return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
  }

  invertExpanded() {
      this.expanded = !this.expanded;
  }

  computeDefaultEnabledUI(componentDescription) {
      return componentDescription.defaultEnabled ? {
          icon: "vaadin:check-circle",
          tooltip: "Default Enabled"
      } : {
          icon: "vaadin:close-circle",
          tooltip: "Default Disabled"
      };
  }
}
window.customElements.define(ScrComponentUnconfiguredComponent.is, ScrComponentUnconfiguredComponent);
