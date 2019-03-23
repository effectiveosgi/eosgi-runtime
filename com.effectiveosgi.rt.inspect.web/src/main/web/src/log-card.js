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
class LogCard extends PolymerElement {
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
      iron-icon.service-icon {
          color: var(--paper-yellow-400);
          transform: rotate(180deg);
      }
      .number { color: var(--paper-blue-600); }
      .light { color: var(--paper-grey-600); }
      .good { color: var(--paper-green-700); }
      .error { color: var(--paper-red-800); }
      .warning { color: var(--paper-yellow-700); }
    </style>
    
    <paper-card>
        <div class="card-content">
            <table class="maxwidth">
                <colgroup><col width="30px">
                </colgroup><thead>
                    <tr class="header-bar">
                        <th><iron-icon icon="[[expandIcon]]" on-click="invertExpanded" class="light"></iron-icon>
                        </th><th>
                            <iron-icon id="levelIcon" icon="[[levelUI.icon]]" class\$="[[levelUI.class]]"></iron-icon>
                            <paper-tooltip for="levelIcon">[[entry.level.name]]</paper-tooltip>
                            [[entry.message]]
                        </th>
                    </tr>
                </thead>
                <tbody hidden\$="[[!expanded]]">
                    <tr>
                        <td></td>
                        <td>
                            <iron-icon icon="vaadin:clock" class="inline"></iron-icon>
                            [[entry.time]]
                        </td>
                    </tr>
                    <tr hidden\$="[[!entry.bundle]]">
                        <td></td>
                        <td>
                            <iron-icon icon="vaadin:cubes" class="inline light"></iron-icon>
                            <code><a href\$="bundles.html#[[entry.bundle.id]]">[[[entry.bundle.id]]]</a> [[entry.bundle.bsn]] <span class="number">[[entry.bundle.version]]</span></code>
                        </td>
                    </tr>
                    <tr hidden\$="[[!entry.service]]">
                        <td></td>
                        <td>
                            <iron-icon icon="vaadin:play" class="inline service-icon"></iron-icon>
                            <code><a href\$="services.html#[[entry.service.id]]">[[[entry.service.id]]]</a> [[entry.service.objectClass]]</code>
                        </td>
                    </tr>
                    <tr hidden\$="[[!entry.exception]]">
                        <td></td>
                        <td>
                            <table>
                                <colgroup>
                                    <col width="30px">
                                </colgroup>
                                <tr>
                                    <td colspan="2">
                                        <iron-icon class="inline error" icon="vaadin:close-circle"></iron-icon>
                                        <code>[[entry.exception.type]]: [[entry.exception.message]]</code>
                                    </td>
                                </tr>
                                <template is="dom-repeat" items="[[entry.exception.stackTrace]]">
                                    <tr>
                                        <td></td>
                                        <td><code>&nbsp;at [[item.class]]#[[item.method]] ([[item.file]]:[[item.lineNumber]])</code></td>
                                    </tr>
                                </template>
                            </table>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </paper-card>
`;
  }

  static get is() { return 'log-card'; }
  static get properties() {
      return {
          entry: Object,
          expanded: {
              type: Boolean,
              value: false
          },
          expandIcon: {
              type: String,
              computed: 'computeExpandIcon(expanded)'
          },
          levelUI: {
              type: String,
              computed: 'computeLevelUI(entry)'
          }
      };
  }
  computeLevelUI(entry) {
      var result = {
          icon: "vaadin:info-circle-o",
          class: "light"
      };
      switch (entry.level.code) {
      case 1:
          result = {
              icon: "vaadin:exclamation-circle-o",
              class: "error"
          };
          break;
      case 2:
          result = {
    		  icon: "vaadin:exclamation-circle-o",
              class: "warning"
          };
          break;
      case 3:
          result = {
              icon: "vaadin:info-circle-o",
              class: "good"
          };
          break;
      case 4:
          result = {
              icon: "vaadin:bug",
              class: "light"
          };
          break;
      }
      return result;
  }
  computeExpandIcon(expanded) {
      return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
  }

  invertExpanded() {
      this.expanded = !this.expanded;
  }
}
window.customElements.define(LogCard.is, LogCard);
