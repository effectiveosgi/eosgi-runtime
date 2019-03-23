import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
/**
 * @customElement
 * @polymer
 */
class ComponentServiceProvided extends PolymerElement {
  static get template() {
    return html`
    <style>
      :host {
        display: block;
      }
      .inline { vertical-align: middle; }
      .maxwidth { width: 100%; }
      iron-icon.inline {
        --iron-icon-width: 16px;
        --iron-icon-height: 16px;
      }
      iron-icon.service-icon {
        color: var(--paper-yellow-400);
        transform: rotate(180deg);
      }
      th, td {
        font-weight: inherit;
        text-align: left;
      }
      .number { color: var(--paper-blue-600); }
      .good { color: var(--paper-green-700); }
      .warning { color: var(--paper-yellow-700); }
      .error { color: var(--paper-red-800); }
    </style>
    
    <table class="maxwidth">
        <colgroup><col width="20px">
        <col width="20px">
        </colgroup><thead>
          <tr>
            <th><iron-icon class="inline" icon="[[expandIcon]]" on-click="invertExpanded"></iron-icon></th>
            <th><iron-icon icon="vaadin:play" class="inline service-icon"></iron-icon></th>
            <th>
              <code><a href\$="services.html#[[service.id]]">[[[service.id]]]</a> [[service.properties.objectClass]]</code>
            </th>
          </tr>
        </thead>
        <tbody hidden\$="[[!expanded]]">
            <tr hidden\$="[[hasUsingBundles]]">
              <td></td>
              <td></td>
              <td>No bundles using.</td>
            </tr>
            <template is="dom-repeat" items="[[service.usingBundles]]">
                <tr>
                  <td></td>
                  <td></td>
                  <td>
                    Used By: <iron-icon icon="vaadin:cubes" class="inline"></iron-icon> <code><a href\$="bundles.html#[[item.bundleId]]">[[[item.bundleId]]]</a> [[item.bundleSymbolicName]] <span class="number">[[item.bundleVersion]]</span></code>
                  </td>
                </tr>
            </template>
        </tbody>
    </table>
`;
  }

  static get is() { return 'scr-service-provided'; }
  static get properties() {
      return {
          service: Object,
          hasUsingBundles: {
              type: Number,
              computed: 'computeHasUsingBundles()'
          },
          expanded: {
              type: Boolean,
              value: false
          },
          expandIcon: {
              type: String,
              computed: 'computeExpandIcon(expanded)'
          },
      };
  }
  invertExpanded() {
      this.expanded = !this.expanded;
  }
  computeExpandIcon(expanded) {
      return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
  }
  computeHasUsingBundles() {
      return this.service.usingBundles != undefined && this.service.usingBundles.length > 0;
  }
}
window.customElements.define(ComponentServiceProvided.is, ComponentServiceProvided);
