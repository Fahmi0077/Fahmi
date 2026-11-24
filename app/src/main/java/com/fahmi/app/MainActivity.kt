
package com.fahmi.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

data class Trigger(
    var name: String,
    var x: Float = 0.5f,
    var y: Float = 0.5f,
    var size: Float = 42f,
    var mode: String = "Tap",
    var interval: Int = 100,
    var duration: Int = 50
)

class MainActivity : Activity() {

    private val triggers = mutableListOf<Trigger>()
    private var selected = 0
    private lateinit var board: TriggerBoard
    private lateinit var status: TextView
    private lateinit var modeSpinner: Spinner
    private lateinit var intervalBar: SeekBar
    private lateinit var durationBar: SeekBar

    private val modes = arrayOf(
        "Tap", "Repeat Tap", "Double Tap", "Release Tap"
    )

    private val bg = Color.rgb(8, 20, 18)
    private val green = Color.rgb(30, 220, 160)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadTriggers()
        if (triggers.isEmpty()) {
            triggers.add(Trigger("Trigger 1", .35f, .5f))
            triggers.add(Trigger("Trigger 2", .65f, .5f))
            triggers.add(Trigger("Trigger 3", .5f, .7f))
        }

        buildInterface()
        selectTrigger(0)
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 16)
            setBackgroundColor(bg)
        }

        val heading = TextView(this).apply {
            text = "F  FAHMI"
            textSize = 28f
            setTextColor(green)
            setPadding(4, 8, 4, 16)
        }
        root.addView(heading)

        status = TextView(this).apply {
            text = "TRIGGER MAPPING"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(4, 8, 4, 12)
        }
        root.addView(status)

        board = TriggerBoard()
        root.addView(
            board,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            ).apply {
                bottomMargin = 12
            }
        )

        val add = Button(this).apply {
            text = "+ ADD TRIGGER"
            setOnClickListener {
                val number = triggers.size + 1
                triggers.add(
                    Trigger("Trigger $number", .5f, .5f)
                )
                selectTrigger(triggers.lastIndex)
                saveTriggers()
                board.invalidate()
            }
        }
        root.addView(add)

        val remove = Button(this).apply {
            text = "REMOVE SELECTED"
            setOnClickListener {
                if (triggers.isNotEmpty()) {
                    triggers.removeAt(selected)
                    selected = (selected - 1).coerceAtLeast(0)
                    saveTriggers()
                    board.invalidate()
                    updateControls()
                }
            }
        }
        root.addView(remove)

        val modeLabel = makeLabel("TRIGGER MODE")
        root.addView(modeLabel)

        modeSpinner = Spinner(this)
        modeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            modes
        )
        modeSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (triggers.isNotEmpty()) {
                        triggers[selected].mode = modes[position]
                        saveTriggers()
                        board.invalidate()
                    }
                }
            }
        root.addView(modeSpinner)

        root.addView(makeLabel("TAP INTERVAL"))
        intervalBar = SeekBar(this).apply {
            max = 2000
            setOnSeekBarChangeListener(simpleListener {
                if (triggers.isNotEmpty()) {
                    triggers[selected].interval = it
                    saveTriggers()
                }
            })
        }
        root.addView(intervalBar)

        root.addView(makeLabel("TAP DURATION"))
        durationBar = SeekBar(this).apply {
            max = 2000
            setOnSeekBarChangeListener(simpleListener {
                if (triggers.isNotEmpty()) {
                    triggers[selected].duration = it
                    saveTriggers()
                }
            })
        }
        root.addView(durationBar)

        val save = Button(this).apply {
            text = "SAVE MAPPING"
            setOnClickListener {
                saveTriggers()
                Toast.makeText(
                    this@MainActivity,
                    "Mapping saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        root.addView(save)

        setContentView(root)
    }

    private fun makeLabel(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(green)
            setPadding(4, 10, 4, 4)
        }
    }

    private fun simpleListener(
        action: (Int) -> Unit
    ) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            bar: SeekBar?, value: Int, user: Boolean
        ) {
            if (user) action(value)
        }
        override fun onStartTrackingTouch(bar: SeekBar?) {}
        override fun onStopTrackingTouch(bar: SeekBar?) {}
    }

    private fun selectTrigger(index: Int) {
        if (triggers.isEmpty()) {
            status.text = "NO TRIGGERS"
            return
        }

        selected = index.coerceIn(0, triggers.lastIndex)
        updateControls()
        board.invalidate()
    }

    private fun updateControls() {
        if (triggers.isEmpty()) {
            status.text = "NO TRIGGERS"
            return
        }

        val t = triggers[selected]
        status.text = "${t.name}  •  ${t.mode}"

        modeSpinner.setSelection(
            modes.indexOf(t.mode).coerceAtLeast(0)
        )

        intervalBar.progress = t.interval.coerceIn(0, 2000)
        durationBar.progress = t.duration.coerceIn(0, 2000)
    }

    private fun saveTriggers() {
        val array = JSONArray()

        triggers.forEach { t ->
            val obj = JSONObject()
            obj.put("name", t.name)
            obj.put("x", t.x.toDouble())
            obj.put("y", t.y.toDouble())
            obj.put("size", t.size.toDouble())
            obj.put("mode", t.mode)
            obj.put("interval", t.interval)
            obj.put("duration", t.duration)
            array.put(obj)
        }

        getSharedPreferences(
            "fahmi_mappings", MODE_PRIVATE
        ).edit().putString("triggers", array.toString()).apply()
    }

    private fun loadTriggers() {
        try {
            val data = getSharedPreferences(
                "fahmi_mappings", MODE_PRIVATE
            ).getString("triggers", null) ?: return

            val array = JSONArray(data)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                triggers.add(
                    Trigger(
                        name = obj.getString("name"),
                        x = obj.getDouble("x").toFloat(),
                        y = obj.getDouble("y").toFloat(),
                        size = obj.getDouble("size").toFloat(),
                        mode = obj.getString("mode"),
                        interval = obj.getInt("interval"),
                        duration = obj.getInt("duration")
                    )
                )
            }
        } catch (_: Exception) {
            triggers.clear()
        }
    }

    inner class TriggerBoard : View(this@MainActivity) {

        private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var active = -1
        private var resizing = false
        private var lastX = 0f
        private var lastY = 0f

        init {
            setBackgroundColor(Color.rgb(14, 32, 28))
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            markerPaint.color = Color.rgb(38, 65, 57)
            markerPaint.strokeWidth = 2f
            markerPaint.style = Paint.Style.STROKE
            canvas.drawRect(
                4f, 4f, width - 4f, height - 4f,
                markerPaint
            )

            triggers.forEachIndexed { index, t ->
                val cx = t.x * width
                val cy = t.y * height

                markerPaint.style = Paint.Style.FILL
                markerPaint.color =
                    if (index == selected) green
                    else Color.rgb(55, 125, 105)

                canvas.drawCircle(
                    cx, cy, t.size, markerPaint
                )

                markerPaint.color = Color.BLACK
                canvas.drawCircle(
                    cx, cy, t.size * .82f, markerPaint
                )

                textPaint.color = Color.WHITE
                textPaint.textSize = 24f
                textPaint.textAlign = Paint.Align.CENTER

                canvas.drawText(
                    t.name.replace("Trigger ", "T"),
                    cx, cy + 8f, textPaint
                )

                if (index == selected) {
                    markerPaint.color = Color.YELLOW
                    canvas.drawCircle(
                        cx + t.size * .72f,
                        cy + t.size * .72f,
                        9f,
                        markerPaint
                    )
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val px = event.x
            val py = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    active = triggers.indices.reversed().firstOrNull {
                        val t = triggers[it]
                        hypot(
                            px - t.x * width,
                            py - t.y * height
                        ) < t.size + 15f
                    } ?: -1

                    if (active >= 0) {
                        selectTrigger(active)
                        val t = triggers[active]
                        val cx = t.x * width
                        val cy = t.y * height

                        resizing = hypot(
                            px - (cx + t.size * .72f),
                            py - (cy + t.size * .72f)
                        ) < 22f

                        lastX = px
                        lastY = py
                        return true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (active >= 0) {
                        val t = triggers[active]

                        if (resizing) {
                            t.size = (
                                t.size + hypot(
                                    px - lastX,
                                    py - lastY
                                )
                            ).coerceIn(20f, 110f)
                        } else {
                            t.x = (px / width).coerceIn(0f, 1f)
                            t.y = (py / height).coerceIn(0f, 1f)
                        }

                        lastX = px
                        lastY = py

                        invalidate()
                        saveTriggers()
                        return true
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    active = -1
                    resizing = false
                    return true
                }
            }

            return true
        }
    }
}
