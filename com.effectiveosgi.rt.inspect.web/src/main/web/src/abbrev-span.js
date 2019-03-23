import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import { FlattenedNodesObserver } from '@polymer/polymer/lib/utils/flattened-nodes-observer.js';
/**
 * @customElement
 * @polymer
 */
class AbbreviatedSpan extends PolymerElement {
  static get template() {
    return html`
    <style>
      .inline {
        vertical-align: middle;
      }
      iron-icon {
        --iron-icon-width: 16px;
        --iron-icon-height: 16px;
        color: var(--paper-red-700);
      }
    </style>

    <span hidden\$="[[showExpanded]]">
      <iron-icon class="inline" icon="vaadin:plus-square-o" on-click="invertExpanded"></iron-icon>[[shortContent]]
    </span>
    <span hidden\$="[[!showExpanded]]">
      <iron-icon hidden\$="[[!big]]" class="inline" icon="vaadin:minus-square-o" on-click="invertExpanded"></iron-icon><slot></slot>
    </span>
`;
  }

  static get is() { return 'abbrev-span'; }
  static get properties() {
      return {
          limit: {
              type: Number,
              value: 100
          },
          
          insertNewlines: {
              type: Boolean,
              value: false
          },

          expanded: {
              type: Boolean,
              value: false
          },
          
          big: {
              type: Boolean,
              value: false
          },
          
          shortContent: {
              type: String,
              value: ""
          },
          
          showExpanded: {
              type: Boolean,
              computed: 'computeShowExpanded(expanded, big)'
          },
      };
  }
  constructor() {
      super();
      this._observer= new FlattenedNodesObserver(this, info => {
          for (var i = 0; i < info.addedNodes.length; i++) {
              var node = info.addedNodes[i];
              var text = node.textContent || "";
              if (text.length > this.limit) {
                  this.big = true;
                  this.shortContent = text.substring(0, this.limit-1) + "..."
                  
                  if (this.insertNewlines) {
                      var newText = ""
                      for (var i = 0; i < text.length; i += this.limit) {
                          newText += text.substring(i, i + this.limit) + "\n";
                      }
                      node.textContent = newText;
                  }
              }
          }
      });
  }
  computeShowExpanded(expanded, big) {
      return (!big) || expanded; 
  }
  invertExpanded() {
      this.expanded = !this.expanded;
  }
}
window.customElements.define(AbbreviatedSpan.is, AbbreviatedSpan);
