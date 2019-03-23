import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import './property-table.js';
import './scr-component-description.js';
import './scr-service-provided.js';
import './scr-service-reference-list.js';
/**
 * @customElement
 * @polymer
 */
class ScrComponentConfigComponent extends PolymerElement {
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
      tr paper-card {
        margin-top: 5px;
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

      .inline {
        vertical-align: middle;
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

      .light { color: var(--paper-grey-600); }
      .good { color: var(--paper-green-700); }
      .error { color: var(--paper-red-800); }
      
      .maxwidth {
        width: 100%;
      }
    </style>

    <paper-card>
      <div class="card-content">
        <table class="maxwidth">
          <colgroup><col width="30px">
          </colgroup><thead>
            <tr class="header-bar">
              <th><iron-icon icon="[[expandIcon]]" on-click="invertExpanded" class="light"></iron-icon>
              </th><th>
                [[[componentConfig.id]]] [[componentConfig.description.name]]
                <div class="header-right">
                    <iron-icon id="stateIcon" icon="[[stateUI.icon]]" class\$="[[stateUI.clazz]]"></iron-icon>
                    <paper-tooltip for="stateIcon">[[stateUI.tooltip]]</paper-tooltip>
                </div>
              </th>
            </tr>
          </thead>

          <tbody hidden\$="[[!expanded]]">
            <!-- Component Description -->
            <tr>
              <td></td>
              <td>
                <paper-card>
                  <div class="card-content">
                  <scr-component-description component-description="[[componentConfig.description]]"></scr-component-description>
                  </div>
                </paper-card>
              </td>
            </tr>

            <!-- Component Runtime Config -->
            <tr>
              <td></td>
              <td>
                <paper-card>
                  <div class="card-content">
                    <table class="maxwidth">
                      <!-- Provided Services -->
                      <tr><td>
                        <scr-service-provided service="[[componentConfig.service]]"></scr-service-provided>
                      </td></tr>
                      <!-- Service References -->
                      <tr><td>
                        <scr-service-reference-list
                            service-refs="[[componentConfig.description.references]]"
                            satisfied-refs="[[componentConfig.satisfiedReferences]]"
                            unsatisfied-refs="[[componentConfig.unsatisfiedReferences]]"
                        ></scr-service-reference-list></td>
                      </td></tr>
                      <!-- Properties -->
                      <tr><td>
                        <property-table properties="[[componentConfig.properties]]" label="component properties"></property-table>
                      </td></tr>
                    </table>
                  </div>
                </paper-card>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </paper-card>
`;
  }

  static get is() { return 'scr-component-config'; }

  static get properties() {
      return {
          componentConfig: Object,

          stateUI: {
              type: String,
              computed: 'computeStateUI()'
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

  computeStateUI() {
      var state = {};
      switch (this.componentConfig.state) {
      case 16: // FAILED_ACTIVATION
        state = {
              icon: "vaadin:warning",
              clazz: "error",
              tooltip: "Activation Failed"
        };
        break;
      case 8: // ACTIVE
          state = {
              icon: "vaadin:check-circle",
              clazz: "good",
              tooltip: "Active"
        };
        break;
      case 4: // SATISFIED
          state = {
              icon: "vaadin:check-circle",
              clazz: "light",
              tooltip: "Satisfied"
        };
        break;
      case 2: // UNSATISFIED_REFERENCE
          state = {
              icon: "vaadin:warning",
              clazz: "error",
              tooltip: "Unsatisfied Reference"
        };
        break;
      case 1: // UNSATISFIED_CONFIGURATION
          state = {
              icon: "vaadin:warning",
              clazz: "error",
              tooltip: "Unsatisfied Config"
        };
        break;
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
window.customElements.define(ScrComponentConfigComponent.is, ScrComponentConfigComponent);
