import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';

import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import './property-table.js';
/**
 * @customElement
 * @polymer
 */
class ConfigurationCard extends PolymerElement {
  static get template() {
    return html`
    <style>
      :host {
        display: block;
      }
      
      paper-card {
        width: 100%;
        min-width: var(--configuration-card-min-width, 1000px);
        background-color: var(--configuration-card-background, white);
      }
      
      .header-bar {
        font-size: 24px;
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
      iron-icon.inline {
        --iron-icon-width: 16px;
        --iron-icon-height: 16px;
      }
      .inline {
        vertical-align: middle;
      }
      .number { color: var(--paper-blue-600); }
      .light { color: var(--paper-grey-600); }
    </style>
    
    <paper-card>
        <div class="card-content">
            <table class="maxwidth">
                <colgroup><col width="30px">
                </colgroup><thead>
                    <tr class="header-bar">
                        <th><iron-icon icon="[[expandIcon]]" on-click="invertExpanded" class="light"></iron-icon>
                        </th><th>
                            <iron-icon icon="vaadin:cog"></iron-icon>
                            <code>[[configuration.pid]]</code>
                            <span class="number">[[[configuration.changeCount]]]</span>
                        </th>
                    </tr>
                </thead>
                <tbody hidden\$="[[!expanded]]">
                    <tr hidden\$="[[!configuration.factoryPid]]">
                        <td></td>
                        <td>
                            <iron-icon icon="vaadin:factory" class="inline light"></iron-icon> Factory PID: <code>[[configuration.factoryPid]]</code>
                        </td>
                    </tr>
                    <tr>
                        <td></td>
                        <td>
                            <iron-icon icon="vaadin:anchor" class="inline light"></iron-icon>
                            Bound To: <code>[[configuration.binding]]</code>
                        </td>
                    </tr>
                    <tr hidden="[[!configuration.boundBundle]]">
                        <td></td>
                        <td>
                            <iron-icon icon="vaadin:cubes" class="inline light"></iron-icon>
                            <code>
                                <a href=\$"bundles.html#[[configuration.boundBundle.id]]">[[[configuration.boundBundle.id]]]</a>
                                [[configuration.boundBundle.bsn]]
                                <span class="number">[[configuration.boundBundle.version]]</span>
                            </code>
                        </td>
                    </tr>
                    <!-- Properties Table -->
                    <tr>
                        <td></td>
                        <td>
                            <property-table expanded properties="[[configuration.properties]]" label="configuration properties"></property-table>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </paper-card>
`;
  }

  static get is() { return 'configuration-card'; }
  static get properties() {
      return {
          configuration: Object,
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
  computeExpandIcon(expanded) {
      return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
  }

  invertExpanded() {
      this.expanded = !this.expanded;
  }
}
window.customElements.define(ConfigurationCard.is, ConfigurationCard);
