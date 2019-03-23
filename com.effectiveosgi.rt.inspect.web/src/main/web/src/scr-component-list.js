import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-ajax/iron-ajax.js';
import '@polymer/paper-spinner/paper-spinner-lite.js';
import './scr-component-config.js';
import './scr-component-unconfigured.js';
/**
 * @customElement
 * @polymer
 */
class ScrComponentListComponent extends PolymerElement {
  static get template() {
    return html`
    <style>
      :host {
        display: block;
      }
      scr-component-config {
        width: 100%;
        margin-top: var(--scr-component-list-margin-between, 10px);
      }
      scr-component-unconfigured {
        width: 100%;
        margin-top: var(--scr-component-list-margin-between, 10px);
      }
    </style>

    <iron-ajax auto="" url="http://localhost:8080/api/scr" handle-as="json" last-response="{{ajaxResponse}}" debounce-duration="300" loading="{{_loading}}"></iron-ajax>

	<span hidden\$="[[!_loading]]">
		<paper-spinner-lite active=""></paper-spinner-lite> Loading...
	</span>
	<span hidden=\$"[[_loading]]">
		<span hidden\$="[[ajaxResponse.status.available]]">SCR not available</span>
	</span>
	<template is="dom-repeat" items="[[ajaxResponse.configured]]">
		<scr-component-config component-config="[[item]]"></scr-component-config>
	</template>
	<template is="dom-repeat" items="[[ajaxResponse.unconfigured]]">
		<scr-component-unconfigured component-description="[[item]]"></scr-component-unconfigured>
	</template>
`;
  }

  static get is() { return 'scr-component-list'; }
  static get properties() {
      return {
          _loading: Boolean
      };
  }
}
window.customElements.define(ScrComponentListComponent.is, ScrComponentListComponent);
