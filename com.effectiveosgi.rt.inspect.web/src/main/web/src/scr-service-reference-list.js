import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
/**
 * @customElement
 * @polymer
 */
class ComponentServiceReference extends PolymerElement {
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
            <th><iron-icon id="satisfiedIcon" icon="[[satisfiedUI.icon]]" class\$="[[satisfiedUI.clazz]]"></iron-icon></th>
            <th>
              <paper-tooltip for="satisfiedIcon">[[satisfiedUI.tooltip]]</paper-tooltip>
              [[serviceRef.name]] —
              <code>[[serviceRef.interfaceName]]</code>
              <code class="number">[[[serviceRef.cardinality]]]</code>
              ([[serviceRef.policy]]+[[serviceRef.policyOption]])
            </th>
          </tr>
        </thead>
        <tbody hidden\$="[[!expanded]]">
            <tr hidden\$="[[hasBoundServices]]">
              <td></td>
              <td></td>
              <td>No services are bound.</td>
            </tr>
            <template is="dom-repeat" items="[[boundServices]]">
                <tr>
                  <td></td>
                  <td></td>
                  <td>
                    Bound To:
                    <a href\$="services.html#[[item.id]]">[[[item.id]]]</a>
                    From:
                    <iron-icon icon="vaadin:cubes" class="inline"></iron-icon>
                    <code><a href\$="bundles.html#[[item.bundleId]]">[[[item.bundleId]]]</a> [[item.bundleSymbolicName]]</code> <code class="number">[[item.bundleVersion]]</code>
                  </td>
                </tr>
            </template>
        </tbody>
    </table>
`;
  }

  static get is() { return 'scr-service-reference'; }
  static get properties() {
      return {
          serviceRef: Object,
          
          satisfied: Boolean,
          
          boundServices: Array,
          
          hasBoundServices: {
              type: Number,
              computed: 'computeHasBoundServices()'
          },

          expanded: {
              type: Boolean,
              value: false
          },
          
          expandIcon: {
              type: String,
              computed: 'computeExpandIcon(expanded)'
          },

          satisfiedUI: {
              type: Object,
              computed: 'computeSatisfiedUI()'
          }
      };
  }
  invertExpanded() {
      this.expanded = !this.expanded;
  }
  computeExpandIcon(expanded) {
      return expanded ? "vaadin:caret-down" : "vaadin:caret-right";
  }
  computeHasBoundServices() {
      return this.boundServices != undefined && this.boundServices.length > 0;
  }
  computeSatisfiedUI() {
      var ui;
      if (this.satisfied) {
          ui = { icon: "vaadin:play", clazz: "good flipped", tooltip: "Satisfied Reference" };
      } else {
          ui = { icon: "vaadin:play", clazz: "error flipped", tooltip: "Unsatisfied Reference" }
      }
      ui.clazz = "inline " + ui.clazz;
      return ui;
  }
}
window.customElements.define(ComponentServiceReference.is, ComponentServiceReference);
/**
 * @customElement
 * @polymer
 */
class ComponentServiceReferenceList extends PolymerElement {
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
      th {
        font-weight: inherit;
        text-align: left;
      }
      .number { color: var(--paper-blue-600); }
      .good { color: var(--paper-green-700); }
      .warning { color: var(--paper-yellow-700); }
      .error { color: var(--paper-red-800); }
    </style>

    <template is="dom-repeat" items="[[satisfiedRefs]]">
        <scr-service-reference satisfied="" service-ref="[[_findServiceReference(item.name)]]" bound-services="[[item.boundServices]]"></scr-service-reference>
    </template>

    <template is="dom-repeat" items="[[unsatisfiedRefs]]">
        <scr-service-reference service-ref="[[_findServiceReference(item.name)]]"></scr-service-reference>
    </template>
`;
  }

  static get is() { return 'scr-service-reference-list'; }
  static get properties() {
      return {
          serviceRefs: Array,
          satisfiedRefs: Array,
          unsatisfiedRefs: Array
      };
  }
  _findServiceReference(name) {
      var i;
      for (i = 0; i < this.serviceRefs.length; i++) {
          var serviceRef = this.serviceRefs[i];
          if (name === serviceRef.name) {
              return serviceRef;
          }
      }
      return { name: name, interfaceName: "<<unknown>>" };
  }
}
window.customElements.define(ComponentServiceReferenceList.is, ComponentServiceReferenceList);
