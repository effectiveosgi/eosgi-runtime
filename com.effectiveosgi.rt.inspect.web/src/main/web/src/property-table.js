import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import './abbrev-span.js';
/**
 * @customElement
 * @polymer
 */
class PropertyTable extends PolymerElement {
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
      }
      .maxwidth {
        width: 100%;
      }
    </style>
    
    <table class="maxwidth">
        <colgroup><col width="20px">
        </colgroup><tbody><tr>
            <td class="expand"><iron-icon class="inline" icon="[[expandIcon]]" on-click="invertExpanded"></iron-icon></td>
            <td hidden\$="[[expanded]]">[[count]] [[label]]…</td>
            <td hidden\$="[[!expanded]]">
              <table class="maxwidth">
                <template is="dom-repeat" items="[[propertiesArray]]">
                  <tr class="properties">
                    <td class="column-left"><code>[[item.name]]</code></td>
                    <td style="max-width: 800px">
                        <code style="overflow-wrap: break-word"><abbrev-span limit="80">[[item.value]]</abbrev-span></code>
                    </td>
                  </tr>
                </template>
              </table>
            </td>
        </tr>
    </tbody></table>
`;
  }

  static get is() { return 'property-table'; }
  static get properties() {
      return {
          properties: Object,
          
          label: {
              type: String,
              value: "properties"
          },
          
          propertiesArray: {
              type: Array,
              computed: 'computePropertiesArray(properties)'
          },
          
          count: {
              type: Number,
              computed: 'computeRowCount(properties)'
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

  // Convert an object such as {a:b, c:d} to an array of name-value pairs
  // e.g. [{name:a, value:b}, {name:c, value:d}]
  computePropertiesArray(obj) {
      var result = Object.keys(obj).map(function (key) {
          return { "name" : key, "value" : obj[key] };
      });
      return result;
  }
  computeRowCount(obj) {
      return Object.keys(obj).length;
  }
  computeExpandIcon(expanded) {
      return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
  }
  invertExpanded() {
      this.expanded = !this.expanded;
  }
}
window.customElements.define(PropertyTable.is, PropertyTable);
