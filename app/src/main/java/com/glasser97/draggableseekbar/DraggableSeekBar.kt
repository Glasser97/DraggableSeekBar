package com.glasser97.draggableseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView


@SuppressLint("UseCompatLoadingForColorStateLists")
class DraggableSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = "ProgressSeekBar"
    }

    private var seekBarDefaultWidth: Int = 0
    private var progressBarRealWidth: Float = 0F
    private var textBgHeight: Int = 0
    private var textBgWidth: Int = 0
    private var playedProgressBarHeight: Int = 0
    private var progressBarHeight: Int = 0

    private var playedProgressBarColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.blue)

    private var progressBarColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.gray_dark)

    private var textColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.deepgray_light)

    private var textBgColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.gray_dark)

    private var disablePlayedProgressBarColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.blue100)

    private var disableProgressBarColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.gray_light)

    private var disableTextColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.gray_dark6)

    private var disableTextBgColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.gray_light)

    private var shadowLayColorRes: ColorStateList =
        context.resources.getColorStateList(R.color.black)

    /**
     * 最大进度，单位为毫秒
     */
    private var maxProgress: Int = 1

    /**
     * 当前进度，单位为毫秒
     */
    private var progress: Int = 0


    private var textSize: Int = 0

    private val playedProgressBarPaint: Paint = Paint()
    private val progressBarPaint: Paint = Paint()
    private val textPaint: Paint = Paint()
    private val textBgPaint: Paint = Paint()

    private val touchSlop = ViewConfiguration.get(getContext()).scaledTouchSlop

    private var tempRect = Rect()

    private var textBgStartX = 0F

    private var textBgDownStartX = 0F

    private var textBgEndX = 0F

    private var textBgTopY = 0F

    private var textBgBottomY = 0F

    private var textHeight: Int = 0

    private var isDragging = false

    private var isDisabled = false

    init {
        // 默认宽度屏幕的宽度-padding
        seekBarDefaultWidth =
            context.resources.displayMetrics.widthPixels - paddingLeft - paddingRight
        textBgWidth = dp2px(70f)
        textBgWidth = dp2px(16f)

        playedProgressBarHeight = dp2px(2f)
        progressBarHeight = dp2px(2f)
        textSize = dp2px(10f)

        progressBarPaint.isAntiAlias = true
        progressBarPaint.style = Paint.Style.FILL

        playedProgressBarPaint.isAntiAlias = true
        playedProgressBarPaint.style = Paint.Style.FILL

        textBgPaint.isAntiAlias = true
        textBgPaint.style = Paint.Style.FILL
        textBgPaint.setShadowLayer(
            dp2px(2f).toFloat(),
            0F,
            dp2px(1f).toFloat(),
            shadowLayColorRes.defaultColor
        )

        textPaint.isAntiAlias = true
        textPaint.style = Paint.Style.FILL
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = textSize.toFloat()
        textHeight = getTextHeight(textPaint, "00:00/00:00")
        textBgHeight = textHeight + dp2px(12F)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 测量宽高
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var width: Int
        var height: Int

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize
        } else {
            width = seekBarDefaultWidth
            if (widthMode == MeasureSpec.AT_MOST) {
                width = width.coerceAtMost(widthSize)
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize
        } else {
            // 当为wrap_content的时候，高度是所有部分最高的那个高度，一般为textBgHeight
            height = textBgHeight
            if (heightMode == MeasureSpec.AT_MOST) {
                height = height.coerceAtMost(heightSize)
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 更新一下画笔的颜色，避免已经换肤了
        updateSettingPaintColor()

        progressBarRealWidth = (width).toFloat() - paddingLeft - paddingRight

        // 判断界限
        if (progress < 0) {
            progress = 0
        }
        if (progress > maxProgress) {
            progress = maxProgress
        }

        // 画背景条
        canvas.drawRoundRect(
            paddingLeft.toFloat(), ((height / 2) - (progressBarHeight / 2)).toFloat(),
            paddingLeft + progressBarRealWidth, ((height / 2) + (progressBarHeight / 2)).toFloat(),
            progressBarHeight.toFloat(), progressBarHeight.toFloat(),
            progressBarPaint
        )

        // 画已播放条
        val currentProgress: Float = progress * progressBarRealWidth / maxProgress
        canvas.drawRoundRect(
            paddingLeft.toFloat(), ((height / 2) - (playedProgressBarHeight / 2)).toFloat(),
            paddingLeft + currentProgress, ((height / 2) + (playedProgressBarHeight / 2)).toFloat(),
            progressBarHeight.toFloat(), progressBarHeight.toFloat(),
            playedProgressBarPaint
        )

        // 计算文字，文字长度随时可能会变，需要每次重绘都测量
        val progressText: String = getProgressText()
        val textWidth: Int = getTextWidth(textPaint, progressText)
        // 画文字背景 背景的宽高也是随文字变化的，这里看要不要把里面文字的Padding给出来可以设置
        textBgWidth = textWidth + dp2px(12F)
        textBgStartX =
            paddingLeft + (progressBarRealWidth - textBgWidth.toFloat()) * progress.toFloat() / maxProgress.toFloat()
        textBgEndX = textBgStartX + textBgWidth

        textBgTopY = ((height / 2) - (textBgHeight / 2)).toFloat()
        textBgBottomY = ((height / 2) + (textBgHeight / 2)).toFloat()

        canvas.drawRoundRect(
            textBgStartX
            , textBgTopY
            , textBgEndX
            , textBgBottomY
            , (textBgWidth / 2).toFloat() //x半径
            , (textBgWidth / 2).toFloat() //y半径
            , textBgPaint
        )

        // 计算绘制文字的位置
        val textStartY: Float = ((height / 2) + (textHeight / 2)).toFloat()
        val textStartX: Float = textBgStartX + (textBgWidth / 2) - (textWidth / 2)
        canvas.drawText(progressText, textStartX, textStartY, textPaint)
    }

    var downX = 0F
    var downY = 0F
    var movedX = 0F
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (isDisabled) {
            return false
        }

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {

                downX = event.x
                downY = event.y
                if (isInTextRect(downX, downY)) {
                    this.parent?.requestDisallowInterceptTouchEvent(true)
                    isDragging = true
                    textBgDownStartX = textBgStartX
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    movedX = event.x - downX
                    touchMovedUpdate()
                    updateWindowPosition()
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isDragging && !isClick(downX, downY, event.x, event.y)) {
                    // 一次滑动结束，更新一下
//                    textBgStartX = textBgDownStartX + movedX
//                    movedX = 0f
                    iProgressListener?.progress(progress)
                }
                isDragging = false
                this.parent?.requestDisallowInterceptTouchEvent(false)
                dismissWindow()
            }
        }

        return true
    }

    private fun touchMovedUpdate() {
        var touchProgress =
            (((textBgDownStartX + movedX - paddingLeft) / (progressBarRealWidth - textBgWidth) * maxProgress).toInt())
        if (touchProgress < 0) {
            touchProgress = 0
        } else if (touchProgress > maxProgress) {
            touchProgress = maxProgress
        }
        progress = touchProgress
        postInvalidate()
    }

    private fun isClick(downX: Float, downY: Float, moveX: Float, moveY: Float): Boolean {
        val offsetX = moveX - downX
        val offsetY = moveY - downY
        return offsetX * offsetX + offsetY * offsetY < touchSlop * touchSlop
    }

    private fun isInTextRect(x: Float, y: Float): Boolean {
        return x > textBgStartX && x < textBgEndX && y > textBgTopY && y < textBgBottomY
    }

    private fun updateWindowPosition() {
        val toastMiddle = textBgStartX + left + textBgWidth / 2
        popProgressText?.text = getProgressText()
        popProgressText?.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val width = popProgressText?.measuredWidth ?: 0
        popProgressText?.translationX = toastMiddle - width / 2 - (popProgressText?.left ?: 0)
        if (popProgressText?.visibility != VISIBLE) {
            popProgressText?.visibility = VISIBLE
        }
    }

    private fun dismissWindow() {
        popProgressText?.visibility = INVISIBLE
    }


    private fun getTextWidth(paint: Paint, str: String): Int {
        paint.getTextBounds(str, 0, str.length, tempRect)
        return tempRect.width()
    }

    private fun getTextHeight(paint: Paint, str: String): Int {
        paint.getTextBounds(str, 0, str.length, tempRect)
        return tempRect.height()
    }

    private fun getProgressText(): String {
        val progressText: String = formatProgress(this.progress)
        val maxProgressText: String = formatProgress(this.maxProgress)
        return "$progressText/$maxProgressText"
    }

    /**
     * @param progress 进度(毫秒)
     */
    fun setProgress(progress: Int) {
        if (isDragging) {
            return
        }
        if (this.progress != progress) {
            this.progress = progress
            postInvalidate()
        }
    }

    /**
     * @param maxProgress 总时长(毫秒)
     */
    fun setMaxProgress(maxProgress: Int) {
        // 因为控件中maxProgress被除，为了避免异常，这里需要做一个兜底。
        var realMaxProgress = maxProgress
        if (maxProgress <= 0) {
            realMaxProgress = 1
        }
        this.maxProgress = realMaxProgress
    }

    fun getMaxProgress(): Int = this.maxProgress

    fun getProgress(): Int = this.progress

    private var iProgressListener: IProgressListener? = null

    fun setProgressListener(iProgressListener: IProgressListener) {
        this.iProgressListener = iProgressListener
    }

    private var popProgressText: TextView? = null

    fun setPopProgressText(popProgressText: TextView) {
        this.popProgressText = popProgressText
    }

    /**
     * @param playedProgressBarHeight 已播放条高度 单位dp
     */
    fun setPlayedProgressBarHeight(playedProgressBarHeight: Float) {
        this.playedProgressBarHeight = dp2px(playedProgressBarHeight)
    }

    /**
     * @param progressBarHeight 进度条高度 单位dp
     */
    fun setProgressBarHeight(progressBarHeight: Float) {
        this.progressBarHeight = dp2px(progressBarHeight)
    }

    fun setPlayedProgressBarColor(playedProgressBarColor: ColorStateList) {
        this.playedProgressBarColorRes = playedProgressBarColor
    }

    fun setProgressBarColor(progressBarColor: ColorStateList) {
        this.progressBarColorRes = progressBarColor
    }

    fun setTextColor(textColor: ColorStateList) {
        this.textColorRes = textColor
    }

    fun setTextBgColor(textBgColor: ColorStateList) {
        this.textBgColorRes = textBgColor
    }

    fun setDisablePlayedProgressBarColor(disablePlayedProgressBarColor: ColorStateList) {
        this.disablePlayedProgressBarColorRes = disablePlayedProgressBarColor
    }

    fun setDisableProgressBarColor(disableProgressBarColor: ColorStateList) {
        this.disableProgressBarColorRes = disableProgressBarColor
    }

    fun setDisableTextColor(disableTextColor: ColorStateList) {
        this.disableTextColorRes = disableTextColor
    }

    fun setDisableTextBgColor(disableTextBgColor: ColorStateList) {
        this.disableTextBgColorRes = disableTextBgColor
    }

    fun setShadowColor(shadowLayColor: ColorStateList) {
        this.shadowLayColorRes = shadowLayColor
    }

    /**
     * @param textSize 文字大小 单位dp
     */
    fun setTextSize(textSize: Int) {
        this.textSize = dp2px(textSize.toFloat())
    }

    /**
     * 设置置灰状态
     * 置灰状态下禁用滑动
     * 更新一下Paint的颜色
     *
     * @param isDisabled true表示禁用，false表示取消禁用
     */
    fun setIsDisabled(isDisabled: Boolean) {
        this.isDisabled = isDisabled
        if (isDisabled) {
            isDragging = false
            dismissWindow()
        }
        postInvalidate()
    }

    /**
     * 获取置灰状态
     */
    fun getIsDisabled(): Boolean {
        return this.isDisabled
    }

    /**
     * 根据当前的状态[isDisabled]更新Paint的颜色
     */
    private fun updateSettingPaintColor() {

        if (!isDisabled) {
            playedProgressBarPaint.color = this.playedProgressBarColorRes.defaultColor
            progressBarPaint.color = this.progressBarColorRes.defaultColor
            textPaint.color = this.textColorRes.defaultColor
            textBgPaint.color = this.textBgColorRes.defaultColor
        } else {
            playedProgressBarPaint.color = this.disablePlayedProgressBarColorRes.defaultColor
            progressBarPaint.color = this.disableProgressBarColorRes.defaultColor
            textPaint.color = this.disableTextColorRes.defaultColor
            textBgPaint.color = this.disableTextBgColorRes.defaultColor
        }
    }

    /**
     * 格式化进度字符串
     *
     * @param mills 进度(毫秒)
     * @return
     */
    private fun formatProgress(mills: Int): String {
        val seconds = mills / 1000
        val standardTime: String
        if (seconds <= 0) {
            standardTime = "00:00"
        } else if (seconds < 60) {
            standardTime = "00:${fillZero(seconds % 60)}"
        } else if (seconds < 3600) {
            standardTime = "${fillZero(seconds / 60)}:${fillZero(seconds % 60)}"
        } else {
            standardTime =
                "${fillZero(seconds / 3600)}:${fillZero(seconds % 3600 / 60)}:${fillZero(seconds % 60)}"
        }
        return standardTime
    }

    private fun fillZero(num: Int): String = if (num < 10) "0$num" else num.toString()

    private fun dp2px(dp: Float): Int {
        return (context.resources.displayMetrics.density * dp + 0.5f).toInt()
    }

    interface IProgressListener {
        /**
         * 进度，单位为毫秒mills
         */
        fun progress(progress: Int)
    }
}