/*
 * markdown_highlight_rules.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
define("mode/rmarkdown_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var c_cppHighlightRules = require("mode/c_cpp_highlight_rules").c_cppHighlightRules;
var MarkdownHighlightRules = require("mode/markdown_highlight_rules").MarkdownHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var YamlHighlightRules = require("ace/mode/yaml_highlight_rules").YamlHighlightRules;
var Utils = require("mode/utils");

function makeDateRegex()
{
   var months = ["January", "February", "March", "April", "May", "June",
                 "July", "August", "September", "October", "November", "December"];

   var reString =
          "((?:" + months.join("|") + ")\\s+\\d+(?:st|nd|rd|s|n|r)?(?:\\s*(?:,)?(?:\\s*\\d+)?)?)";

   return new RegExp(reString);
}

var RMarkdownHighlightRules = function() {

   // Base rule set (markdown)
   this.$rules = new MarkdownHighlightRules().getRules();
   
   // Embed R highlight rules
   Utils.embedRules(
      this,
      RHighlightRules,
      "r",
      this.$reRChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed C++ highlight rules
   Utils.embedRules(
      this,
      c_cppHighlightRules,
      "r-cpp",
      this.$reCppChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed YAML highlight rules, but ensure that they can only be
   // found at the start of a document. We do this by moving all of the
   // start rules to a second '$start' state, and ensuring that YAML headers
   // can only be encountered in the initial 'start' state.
   this.$rules["$start"] = this.$rules["start"].slice();
   for (var key in this.$rules)
   {
      var stateRules = this.$rules[key];
      for (var i = 0; i < stateRules.length; i++)
      {
         if (stateRules[i].next === "start")
            stateRules[i].next = "$start";
      }
   }
   
   Utils.embedRules(
      this,
      YamlHighlightRules,
      "yaml",
      "---",
      "(?:---|\\.\\.\\.)"
   );

   this.$rules["yaml-start"].unshift({
      token: ["string"],
      regex: makeDateRegex()
   });

};
oop.inherits(RMarkdownHighlightRules, TextHighlightRules);

(function() {
   
   this.$reRChunkStartString =
      "^(?:[ ]{4})?`{3,}\\s*\\{[Rr](.*)\\}\\s*$";

   this.$reCppChunkStartString =
      "^(?:[ ]{4})?`{3,}\\s*\\{[Rr](?:.*)engine\\s*\\=\\s*['\"]Rcpp['\"](?:.*)\\}\\s*$|" +
      "^(?:[ ]{4})?`{3,}\\s*\\{[Rr]cpp(?:.*)\\}\\s*$";

   this.$reChunkEndString =
      "^(?:[ ]{4})?`{3,}\\s*$";
   
}).call(RMarkdownHighlightRules.prototype);

exports.RMarkdownHighlightRules = RMarkdownHighlightRules;
});
