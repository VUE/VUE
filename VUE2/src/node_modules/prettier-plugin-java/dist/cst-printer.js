"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createPrettierDoc = void 0;
var base_cst_printer_1 = require("./base-cst-printer");
var arrays_1 = require("./printers/arrays");
var blocks_and_statements_1 = require("./printers/blocks-and-statements");
var classes_1 = require("./printers/classes");
var expressions_1 = require("./printers/expressions");
var interfaces_1 = require("./printers/interfaces");
var lexical_structure_1 = require("./printers/lexical-structure");
var names_1 = require("./printers/names");
var types_values_and_variables_1 = require("./printers/types-values-and-variables");
var packages_and_modules_1 = require("./printers/packages-and-modules");
// Mixins for the win
mixInMethods(arrays_1.ArraysPrettierVisitor, blocks_and_statements_1.BlocksAndStatementPrettierVisitor, classes_1.ClassesPrettierVisitor, expressions_1.ExpressionsPrettierVisitor, interfaces_1.InterfacesPrettierVisitor, lexical_structure_1.LexicalStructurePrettierVisitor, names_1.NamesPrettierVisitor, types_values_and_variables_1.TypesValuesAndVariablesPrettierVisitor, packages_and_modules_1.PackagesAndModulesPrettierVisitor);
function mixInMethods() {
    var classesToMix = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        classesToMix[_i] = arguments[_i];
    }
    classesToMix.forEach(function (from) {
        var fromMethodsNames = Object.getOwnPropertyNames(from.prototype);
        var fromPureMethodsName = fromMethodsNames.filter(function (methodName) { return methodName !== "constructor"; });
        fromPureMethodsName.forEach(function (methodName) {
            // @ts-ignore
            base_cst_printer_1.BaseCstPrettierPrinter.prototype[methodName] = from.prototype[methodName];
        });
    });
}
var prettyPrinter = new base_cst_printer_1.BaseCstPrettierPrinter();
// TODO: do we need the "path" and "print" arguments passed by prettier
// see https://github.com/prettier/prettier/issues/5747
function createPrettierDoc(cstNode, options) {
    prettyPrinter.prettierOptions = options;
    return prettyPrinter.visit(cstNode);
}
exports.createPrettierDoc = createPrettierDoc;
