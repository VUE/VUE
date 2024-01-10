"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.isOrdinaryCompilationUnitCtx = exports.isAnnotationCstNode = exports.isTypeArgumentsCstNode = exports.isCstElementOrUndefinedIToken = exports.isIToken = exports.isCstNode = void 0;
function isCstNode(tokenOrNode) {
    return !isIToken(tokenOrNode);
}
exports.isCstNode = isCstNode;
function isIToken(tokenOrNode) {
    return (tokenOrNode.tokenType !== undefined &&
        tokenOrNode.image !== undefined);
}
exports.isIToken = isIToken;
function isCstElementOrUndefinedIToken(tokenOrNode) {
    return tokenOrNode !== undefined && isIToken(tokenOrNode);
}
exports.isCstElementOrUndefinedIToken = isCstElementOrUndefinedIToken;
var isTypeArgumentsCstNode = function (cstElement) {
    return cstElement.name === "typeArguments";
};
exports.isTypeArgumentsCstNode = isTypeArgumentsCstNode;
var isAnnotationCstNode = function (cstElement) {
    return cstElement.name === "annotation";
};
exports.isAnnotationCstNode = isAnnotationCstNode;
var isOrdinaryCompilationUnitCtx = function (ctx) {
    return (ctx.ordinaryCompilationUnit !==
        undefined);
};
exports.isOrdinaryCompilationUnitCtx = isOrdinaryCompilationUnitCtx;
