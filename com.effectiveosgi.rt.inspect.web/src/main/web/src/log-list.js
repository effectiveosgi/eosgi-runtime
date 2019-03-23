import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-ajax/iron-ajax.js';
import '@polymer/iron-location/iron-location.js';
import '@polymer/iron-location/iron-query-params.js';
import '@polymer/paper-spinner/paper-spinner-lite.js';
import '@polymer/neon-animation/neon-animation.js';
import '@polymer/paper-dropdown-menu/paper-dropdown-menu.js';
import '@polymer/paper-radio-button/paper-radio-button.js';
import '@polymer/paper-radio-group/paper-radio-group.js';
import '@polymer/paper-listbox/paper-listbox.js';

import './log-card.js';

/**
 * @customElement
 * @polymer
 */
class LogListComponent extends PolymerElement {
  static get template() {
    return html`<style>
  :host {
    display: block;
  }
  .card-max {
    width: 100%;
    margin-top: var(--log-list-margin-between, 10px);
  }
  paper-card.filter-card {
    background-color: var(--paper-grey-100);
  }
  .flex-column {
    display: flex;
    flex-flow: column;
  }
  .flex-row {
    display: flex;
    flex-flow: row nowrap;
  }
  .flex-greedy {
    flex-grow: 1;
  }
  .flex-reluctant {
    flex-grow: 0;
  }
  div.card-content > * {
    margin: 0 10px;
  }
</style>

<iron-ajax auto="" url="http://localhost:8080/api/log" handle-as="json" last-response="{{ajaxResponse}}"
  debounce-duration="300" loading="{{_loading}}"></iron-ajax>

<paper-card class="card-max filter-card">
  <div class="card-content flex-row">
      <paper-input label="Search" placeholder="Enter search string" type="search" value="{{searchString}}" class="flex-greedy"></paper-input>
      <paper-dropdown-menu label="Detail Level" noink no-animations dynamic-alignclass="flex-reluctant">
        <paper-radio-group slot="dropdown-content" class="dropdown-content flex-column" selected="{{selectedLevel}}">
          <paper-radio-button name="4">DEBUG</paper-radio-button>
          <paper-radio-button name="3">INFO</paper-radio-button>
          <paper-radio-button name="2">WARNING</paper-radio-button>
          <paper-radio-button name="1">ERROR</paper-radio-button>
        </paper-radio-group>
      </paper-dropdown-menu>
  </div>
</paper-card>


<span hidden\$="[[!_loading]]">
  <paper-spinner-lite class="inline" active=""></paper-spinner-lite>
</span>

<template is="dom-repeat" items="[[ajaxResponse]]" filter="{{computeFilter(selectedLevel, searchString)}}">
  <log-card entry="[[item]]" class="card-max"></log-card>
</template>`;
  }

  static get is() { return 'log-list'; }
  static get properties() {
      return {
        queryParams: Object,
        queryString: String,
        selectedLevel: {
            type: Number,
            value: 3
        },
        _loading: Boolean
      };
  }

  computeFilter(level, searchString) {
    return function (item) {
      var result = level >= item.level.code;
      if (searchString) {
        result = result && (item.message.toLowerCase().indexOf(searchString.toLowerCase()) != -1);
      }
      return result;
    }
  }
}
window.customElements.define(LogListComponent.is, LogListComponent);
