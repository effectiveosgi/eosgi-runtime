import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-ajax/iron-ajax.js';
import '@polymer/iron-location/iron-location.js';
import '@polymer/iron-location/iron-query-params.js';
import '@polymer/paper-spinner/paper-spinner-lite.js';
import './configuration-card.js';

/**
 * @customElement
 * @polymer
 */
class ConfigurationListComponent extends PolymerElement {
  static get template() {
    return html`
    <style>
      :host {
        display: block;
      }
      service-card {
        width: 100%;
        margin-top: var(--bundle-list-margin-between, 10px);
      }
      service-card.selected {
        --service-card-background: var(--paper-orange-50);
      }
    </style>

	<iron-ajax
		auto=""
		url="http://localhost:8080/api/config"
		handle-as="json"
		last-response="{{ajaxResponse}}"
		debounce-duration="300"
		loading="{{_loading}}"
	></iron-ajax>

    <span hidden\$="[[!_loading]]"><paper-spinner-lite class="inline" active=""></paper-spinner-lite></span>

    <template is="dom-repeat" items="[[ajaxResponse]]" on-dom-change="handleListChanged">
        <configuration-card
            id\$="confgiguration-card-[[item.pid]]"
            configuration="[[item]]"
            expanded="[[computeExpanded(item, selectedId)]]"
            class\$="[[computeClass(item, selectedId)]]"
        ></configuration-card>
        </a>
    </template>

`;
  }

  static get is() { return 'configuration-list'; }
  static get properties() {
      return {
        selectedId: String,
        queryParams: Object,
        queryString: String,
        _loading: Boolean
      };
  }
  computeClass(service, selectedId) {
      return service.id == selectedId ? "selected" : "";
  }
  computeExpanded(service, selectedId) {
      return service.id == selectedId;
  }
  scrollToService(serviceId) {
    var target = this.shadowRoot.querySelector("#service-card-" + serviceId);
    if (target) target.scrollIntoView({
      behavior : "smooth", block : "center"
    });
  }
  handleHashChanged(event) {
    var serviceId = event.detail.value;
    if (serviceId && serviceId.length > 0) this.scrollToService(serviceId);
  }
  handleListChanged() {
    this.scrollToService(this.selectedId);
  }
}
window.customElements.define(ConfigurationListComponent.is, ConfigurationListComponent);
