import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-ajax/iron-ajax.js';
import '@polymer/iron-location/iron-location.js';
import '@polymer/iron-location/iron-query-params.js';
import '@polymer/paper-spinner/paper-spinner-lite.js';
import './bundle-card.js';
/**
 * @customElement
 * @polymer
 */
class BundleListComponent extends PolymerElement {
  static get template() {
    return html`
    <style>
      :host {
        display: block;
      }
      bundle-card {
        width: 100%;
        margin-top: var(--bundle-list-margin-between, 10px);
      }
      bundle-card.selected {
        --bundle-card-background: var(--paper-orange-50);
      }
    </style>

    <iron-ajax auto=""
        url="http://localhost:8080/api/bundles"
        handle-as="json"
        last-response="{{ajaxResponse}}"
        debounce-duration="300"
        loading="{{_loading}}"
    ></iron-ajax>

    <iron-location
        hash="{{selectedId}}"
        url-space-regex="/bundles.html"
        on-hash-changed="handleHashChanged"
    ></iron-location>

    <span hidden\$="[[!_loading]]"><paper-spinner-lite class="inline" active=""></paper-spinner-lite></span>
    <template id="bundle_card_list" is="dom-repeat" items="[[ajaxResponse]]" on-dom-change="handleListChanged">
        <bundle-card
            id\$="bundle-card-[[item.id]]"
            bundle="[[item]]"
            class\$="[[computeClass(item, selectedId)]]"
            expanded="[[computeExpanded(item, selectedId)]]"
        ></bundle-card>
        </div>
    </template>
`;
  }

  static get is() { return 'bundle-list'; }
  static get properties() {
      return {
        bundles: Array,
        queryParams: Object,
        queryString: String,
        _loading: Boolean,
        selectedId: String
      };
  }
  computeClass(bundle, selectedId) {
      return bundle.id == selectedId ? "selected" : "";
  }
  computeExpanded(bundle, selectedId) {
      return bundle.id == selectedId;
  }
  scrollToBundleId(bundleId) {
    var target = this.shadowRoot.querySelector("#bundle-card-" + bundleId);
    if (target) target.scrollIntoView({
      behavior : "smooth", block : "center"
    });
  }
  handleHashChanged(event) {
    var bundleId = event.detail.value;
    if (bundleId && bundleId.length > 0) this.scrollToBundleId(bundleId);
  }
  handleListChanged(event) {
    this.scrollToBundleId(this.selectedId);
  }
}
window.customElements.define(BundleListComponent.is, BundleListComponent);
