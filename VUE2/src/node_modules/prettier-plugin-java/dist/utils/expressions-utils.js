"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.isSingleArgumentLambdaExpressionWithBlock = exports.isArgumentListSingleLambda = void 0;
function isArgumentListSingleLambda(argumentList) {
    if (argumentList === undefined) {
        return false;
    }
    var args = argumentList[0].children.expression;
    if (args.length !== 1) {
        return false;
    }
    var argument = args[0];
    return argument.children.lambdaExpression !== undefined;
}
exports.isArgumentListSingleLambda = isArgumentListSingleLambda;
var isSingleArgumentLambdaExpressionWithBlock = function (argumentList) {
    if (argumentList === undefined) {
        return false;
    }
    var args = argumentList[0].children.expression;
    if (args.length !== 1) {
        return false;
    }
    var argument = args[0];
    return (argument.children.lambdaExpression !== undefined &&
        argument.children.lambdaExpression[0].children.lambdaBody[0].children
            .block !== undefined);
};
exports.isSingleArgumentLambdaExpressionWithBlock = isSingleArgumentLambdaExpressionWithBlock;
