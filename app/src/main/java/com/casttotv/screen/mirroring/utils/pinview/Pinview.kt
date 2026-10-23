package com.casttotv.screen.mirroring.utils.pinview

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.casttotv.screen.mirroring.R

/** One-character-per-box PIN entry used by the TV pairing dialogs (attrs: `Pinview`). */
class Pinview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), TextWatcher, View.OnFocusChangeListener, View.OnKeyListener {

    enum class Type { TEXT, NUMBER }

    private val boxes = ArrayList<EditText>()
    private val density = resources.displayMetrics.density.toInt()
    private var pinLength = 4
    private var pinWidth = 50 * density
    private var pinHeight = 50 * density
    private var splitWidth = 20 * density
    private var textSizeSp = 12
    private var cursorVisible = false
    private var password = false
    private var forceKeyboard = true
    private var hint: String? = ""
    private var inputType = Type.TEXT
    private var pinBackground = R.drawable.sample_background

    private var currentFocusBox: View? = null
    private var fromBackspace = false
    private var lastDeleteClearsOnly = false
    private var settingValue = false
    private var clickListener: OnClickListener? = null

    /** Called with the whole value once every box is filled. */
    var onPinEntered: ((String) -> Unit)? = null

    val value: String get() = boxes.joinToString("") { it.text.toString() }

    init {
        gravity = Gravity.CENTER
        orientation = HORIZONTAL
        setWillNotDraw(false)
        context.withStyledAttributes(attrs, R.styleable.Pinview, defStyleAttr, 0) {
            pinBackground = getResourceId(R.styleable.Pinview_pinBackground, pinBackground)
            pinLength = getInt(R.styleable.Pinview_pinLength, pinLength)
            pinHeight = getDimension(R.styleable.Pinview_pinHeight, pinHeight.toFloat()).toInt()
            pinWidth = getDimension(R.styleable.Pinview_pinWidth, pinWidth.toFloat()).toInt()
            splitWidth = getDimension(R.styleable.Pinview_splitWidth, splitWidth.toFloat()).toInt()
            textSizeSp = getDimension(R.styleable.Pinview_textSize, textSizeSp.toFloat()).toInt()
            cursorVisible = getBoolean(R.styleable.Pinview_cursorVisible, cursorVisible)
            password = getBoolean(R.styleable.Pinview_password, password)
            forceKeyboard = getBoolean(R.styleable.Pinview_forceKeyboard, forceKeyboard)
            hint = getString(R.styleable.Pinview_hint)
            inputType = Type.entries[getInt(R.styleable.Pinview_inputType, 0)]
        }
        createBoxes()
        super.setOnClickListener { focusAndShowKeyboard() }
        boxes.firstOrNull()?.postDelayed({ focusAndShowKeyboard() }, 200)
        updateEnabled()
    }

    fun setValue(text: String) {
        if (inputType == Type.NUMBER && !text.matches(Regex("[0-9]*")) || boxes.isEmpty()) return
        settingValue = true
        var last = -1
        boxes.forEachIndexed { i, box ->
            box.setText(text.getOrNull(i)?.toString() ?: "")
            if (i < text.length) last = i
        }
        currentFocusBox = boxes[pinLength - 1]
        if (last == pinLength - 1) {
            if (inputType == Type.NUMBER || password) lastDeleteClearsOnly = true
            onPinEntered?.invoke(value)
        }
        currentFocusBox?.requestFocus()
        settingValue = false
        updateEnabled()
    }

    override fun setOnClickListener(l: OnClickListener?) {
        clickListener = l
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBoxes() {
        removeAllViews()
        boxes.clear()
        val params = LayoutParams(pinWidth, pinHeight).apply { val m = splitWidth / 2; setMargins(m, m, m, m) }
        repeat(pinLength) { i ->
            val box = EditText(context).apply {
                textSize = textSizeSp.toFloat()
                filters = arrayOf(InputFilter.LengthFilter(1))
                layoutParams = params
                gravity = Gravity.CENTER
                isCursorVisible = this@Pinview.cursorVisible
                if (!this@Pinview.cursorVisible) {
                    isClickable = false
                    hint = this@Pinview.hint
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            clickListener?.onClick(this@Pinview)
                            focusAndShowKeyboard()
                        }
                        false
                    }
                }
                setBackgroundResource(pinBackground)
                setPadding(0, 0, 0, 0)
                tag = i.toString()
                inputType = keyboardInputType()
                addTextChangedListener(this@Pinview)
                onFocusChangeListener = this@Pinview
                setOnKeyListener(this@Pinview)
                if (password) transformationMethod = PasswordTransformationMethod.getInstance()
            }
            boxes += box
            addView(box)
        }
    }

    private fun keyboardInputType() = when (inputType) {
        Type.NUMBER -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        Type.TEXT -> InputType.TYPE_CLASS_TEXT
    }

    private fun focusAndShowKeyboard() {
        val target = boxes.firstOrNull { it.length() == 0 } ?: boxes.lastOrNull() ?: return
        target.requestFocus()
        if (forceKeyboard) {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private val focusIndex get() = boxes.indexOf(currentFocusBox)

    private fun updateEnabled() {
        val max = maxOf(0, focusIndex)
        boxes.forEachIndexed { i, box -> box.isEnabled = i <= max }
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        when {
            hasFocus && cursorVisible -> currentFocusBox = v
            !hasFocus -> v.clearFocus()
            fromBackspace -> { currentFocusBox = v; fromBackspace = false }
            else -> {
                val firstEmpty = boxes.firstOrNull { it.length() == 0 } ?: boxes.last()
                if (firstEmpty != v) firstEmpty.requestFocus() else currentFocusBox = v
            }
        }
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP || keyCode != KeyEvent.KEYCODE_DEL) return false
        val index = focusIndex.takeIf { it >= 0 } ?: return false
        val box = boxes[index]
        when {
            (inputType == Type.NUMBER || password) && index == pinLength - 1 && lastDeleteClearsOnly -> {
                box.setText("")
                lastDeleteClearsOnly = false
            }
            index > 0 -> {
                fromBackspace = true
                if (box.text.isEmpty()) boxes[index - 1].requestFocus()
                box.setText("")
            }
            else -> box.setText("")
        }
        return true
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.length == 1 && currentFocusBox != null) {
            val index = focusIndex
            if (index < pinLength - 1) {
                postDelayed({
                    boxes[index + 1].isEnabled = true
                    boxes[index + 1].requestFocus()
                }, if (password) 25L else 1L)
            }
            if (index == pinLength - 1 && (inputType == Type.NUMBER || password)) lastDeleteClearsOnly = true
        } else if (s.isEmpty() && focusIndex >= 0) {
            fromBackspace = true
            boxes[focusIndex].takeIf { it.text.isNotEmpty() }?.setText("")
        }
        if (!settingValue && boxes.all { it.text.isNotEmpty() }) onPinEntered?.invoke(value)
        updateEnabled()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

    override fun afterTextChanged(s: Editable) = Unit
}
