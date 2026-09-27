package com.example.calorietracker.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

val FieldShape = RoundedCornerShape(16.dp)

/** Soft tinted container with a thin outline that turns into the accent colour on focus. */
@Composable
fun appFieldColors(): TextFieldColors {
    val c = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = c.surfaceContainerHighest.copy(alpha = 0.55f),
        focusedContainerColor = c.primaryContainer.copy(alpha = 0.22f),
        disabledContainerColor = c.surfaceContainerHighest.copy(alpha = 0.3f),
        errorContainerColor = c.errorContainer.copy(alpha = 0.18f),
        unfocusedBorderColor = c.outlineVariant.copy(alpha = 0.6f),
        focusedBorderColor = c.primary,
        unfocusedLeadingIconColor = c.primary,
        focusedLeadingIconColor = c.primary
    )
}

/** The app's text field: same parameters as [OutlinedTextField], with the shared look. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = FieldShape,
    colors: TextFieldColors = appFieldColors()
) = OutlinedTextField(
    value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled, readOnly = readOnly,
    textStyle = textStyle, label = label, placeholder = placeholder, leadingIcon = leadingIcon,
    trailingIcon = trailingIcon, prefix = prefix, suffix = suffix, supportingText = supportingText,
    isError = isError, visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
    shape = shape, colors = colors
)

/**
 * Number input that never rewrites what the user is typing. Values outside [range] are
 * shown as an error and not passed on (so erasing "55" to "5" doesn't snap to a minimum);
 * the text is only re-synced from [value] while the field is not focused.
 */
@Composable
fun NumberField(
    label: String,
    value: Double,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    range: ClosedFloatingPointRange<Double>? = null,
    decimals: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next
) {
    var text by remember { mutableStateOf(formatGrams(value)) }
    var focused by remember { mutableStateOf(false) }
    val parsed = text.toNumberOrNull()
    val valid = parsed != null && (range == null || parsed in range)

    // External changes (another screen, a "recalculate" button) show up when not typing.
    LaunchedEffect(value) {
        if (!focused && parsed != value) text = formatGrams(value)
    }

    AppTextField(
        value = text,
        onValueChange = { raw ->
            var sep = false
            val clean = raw.filter { ch ->
                when {
                    ch.isDigit() -> true
                    decimals && (ch == ',' || ch == '.') && !sep -> { sep = true; true }
                    else -> false
                }
            }.take(7)
            text = clean
            clean.toNumberOrNull()?.takeIf { range == null || it in range }?.let(onValue)
        },
        label = { Text(label) },
        enabled = enabled,
        leadingIcon = icon?.let { { Icon(it, null) } },
        suffix = unit?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        isError = text.isNotEmpty() && !valid,
        supportingText = if (text.isNotEmpty() && !valid && range != null) {
            { Text("от ${formatGrams(range.start)} до ${formatGrams(range.endInclusive)}") }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = imeAction
        ),
        modifier = modifier.onFocusChanged { state ->
            if (focused && !state.isFocused && !valid) text = formatGrams(value)
            focused = state.isFocused
        }
    )
}

/**
 * Single-line text that shrinks its font to fit the width it gets (large system fonts, narrow
 * segments). A plain Layout, so parents that ask for intrinsic sizes (segmented buttons) work.
 */
@Composable
fun FitText(text: String, style: TextStyle, modifier: Modifier = Modifier, minSizeSp: Float = 8f) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val color = style.color.takeOrElse { androidx.compose.material3.LocalContentColor.current }
    val holder = remember { arrayOfNulls<androidx.compose.ui.text.TextLayoutResult>(1) }
    androidx.compose.ui.layout.Layout(
        content = {},
        modifier = modifier.semantics { contentDescription = text }
            .drawBehind { holder[0]?.let { drawText(it, color = color) } }
    ) { _, c ->
        var r = measurer.measure(text, style, maxLines = 1, softWrap = false)
        if (c.hasBoundedWidth && r.size.width > c.maxWidth && r.size.width > 0) {
            val size = maxOf(style.fontSize.value * c.maxWidth / r.size.width * 0.97f, minSizeSp)
            r = measurer.measure(text, style.copy(fontSize = size.sp), maxLines = 1, softWrap = false)
        }
        holder[0] = r
        layout(minOf(r.size.width, if (c.hasBoundedWidth) c.maxWidth else r.size.width), r.size.height) {}
    }
}
