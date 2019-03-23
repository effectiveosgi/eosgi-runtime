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
class ServiceCard extends PolymerElement {
  static get template() {
    return html`
    <style>
      :host {
        display: block;
      }
      
      paper-card {
        width: 100%;
        min-width: var(--service-card-min-width, 1000px);
        background-color: var(--service-card-background, white);
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
      iron-icon.inline {
        --iron-icon-width: 16px;
        --iron-icon-height: 16px;
      }
      .inline {
        vertical-align: middle;
      }

      .maxwidth {
        width: 100%;
      }

      .number { color: var(--paper-blue-600); }
      .light { color: var(--paper-grey-600); }
      iron-icon.service-icon {
          color: var(--paper-yellow-400);
          transform: rotate(180deg);
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
                            <iron-icon icon="vaadin:play" class="service-icon"></iron-icon>
                            [[[service.id]]] <code>[[service.properties.objectClass]]</code>
                        </th>
                    </tr>
                </thead>
                <tbody hidden\$="[[!expanded]]">
                    <tr>
                        <td></td>
                        <td>
                            Provided By:
                            <iron-icon class="inline light" icon="vaadin:cubes" id="providerBundleIcon"></iron-icon>
                            <code><a href\$="bundles.html#[[service.bundleId]]">[[[service.bundleId]]]</a> [[service.bundleSymbolicName]]</code> <span class="number">[[service.bundleVersion]]</span>
                        </td>
                    </tr>
                    <!-- Using Bundles -->
                    <template is="dom-repeat" items="[[service.usingBundles]]">
                        <tr>
                            <td></td>
                            <td>
                                Used By:
                                <iron-icon icon="vaadin:cubes" class="inline"></iron-icon>
                                <code><a href\$="bundles.html#[[item.bundleId]]">[[[item.bundleId]]]</a> [[item.bundleSymbolicName]] <span class="number">[[item.bundleVersion]]</span></code>
                            </td>
                        </tr>
                    </template>
                    <!-- Service Properties Table -->
                    <tr>
                        <td></td>
                        <td>
                            <property-table properties="[[service.properties]]" label="service properties"></property-table>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </paper-card>
`;
  }

  static get is() { return 'service-card'; }
  static get properties() {
      return {

          service: Object,

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
window.customElements.define(ServiceCard.is, ServiceCard);
