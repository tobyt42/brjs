/**
 * @module br/validation/EmailValidator
 */

/**
 * @implements module:br/validation/EmailValidator
 * @class
 * @alias module:br/validation/EmailValidator
 */
br.validation.EmailValidator = function()
{
	// This regex has no false negatives, but does have quite a few false positives.
	// see http://www.pgregg.com/projects/php/code/showvalidemail.php
	this.partsRegex = /^[^\.@].*@[^@.-][^@]*/;

	// Regex explanation: must start with a non dot or @ character, followed by none or more of
	// of any character up to the last @ symbol. After the last @ symbol, the next section must start with
	// a character other than @ . or -, and may be followed by none or more of anything except @.
};

br.Core.implement(br.validation.EmailValidator, br.validation.Validator);

br.validation.EmailValidator.prototype.validate = function(vValue, mAttributes, oValidationResult)
{
	oValidationResult.setResult(vValue.match(this.partsRegex) != null, "Invalid E-Mail address.");
};
