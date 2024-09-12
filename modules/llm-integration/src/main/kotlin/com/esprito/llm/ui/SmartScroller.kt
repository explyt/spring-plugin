package com.esprito.llm.ui

import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import javax.swing.JScrollBar
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.text.DefaultCaret
import javax.swing.text.JTextComponent

class SmartScroller @JvmOverloads constructor(
    scrollPane: JScrollPane,
    scrollDirection: Int = VERTICAL,
    viewportPosition: Int = END
) : AdjustmentListener {
    private val viewportPosition: Int

    private var adjustScrollBar = true
    private var previousValue = -1
    private var previousMaximum = -1

    /**
     * Specify how the SmartScroller will function.
     *
     * @param scrollPane       the scroll pane to monitor
     * @param scrollDirection  indicates which JScrollBar to monitor. Valid values are HORIZONTAL and
     * VERTICAL.
     * @param viewportPosition indicates where the viewport will normally be positioned as data is
     * added. Valid values are START and END
     */
    /**
     * Convenience constructor. Scroll direction is VERTICAL and viewport position is at the END.
     *
     * @param scrollPane the scroll pane to monitor
     */
    init {
        require(
            !(scrollDirection != HORIZONTAL
                    && scrollDirection != VERTICAL)
        ) { "invalid scroll direction specified" }

        require(
            !(viewportPosition != START
                    && viewportPosition != END)
        ) { "invalid viewport position specified" }

        this.viewportPosition = viewportPosition
        val scrollBar = if (scrollDirection == HORIZONTAL) {
            scrollPane.horizontalScrollBar
        } else {
            scrollPane.verticalScrollBar
        }

        scrollBar.addAdjustmentListener(this)

        //  Turn off automatic scrolling for text components
        if (scrollPane.viewport.view is JTextComponent) {
            val textComponent = scrollPane.viewport.view as? JTextComponent
            val caret = textComponent?.caret as? DefaultCaret
            caret?.updatePolicy = DefaultCaret.NEVER_UPDATE
        }
        /*
        *
        *  if (scrollPane.getViewport().getView() instanceof JTextComponent textComponent) {
      DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
      caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }
        *
        * */
    }


    override fun adjustmentValueChanged(e: AdjustmentEvent) {
        SwingUtilities.invokeLater { checkScrollBar(e) }
    }

    /*
     *  Analyze every adjustment event to determine when the viewport
     *  needs to be repositioned.
     */
    private fun checkScrollBar(e: AdjustmentEvent) {
        //  The scroll bar listModel contains information needed to determine
        //  whether the viewport should be repositioned or not.

        val scrollBar = e.source as JScrollBar
        val listModel = scrollBar.model
        var value = listModel.value
        val extent = listModel.extent
        val maximum = listModel.maximum

        val valueChanged = previousValue != value
        val maximumChanged = previousMaximum != maximum

        //  Check if the user has manually repositioned the scrollbar
        if (valueChanged && !maximumChanged) {
            adjustScrollBar = if (viewportPosition == START) {
                value != 0
            } else {
                value + extent >= maximum
            }
        }

        //  Reset the "value" so we can reposition the viewport and
        //  distinguish between a user scroll and a program scroll.
        //  (ie. valueChanged will be false on a program scroll)
        if (adjustScrollBar && viewportPosition == END) {
            //  Scroll the viewport to the end.
            scrollBar.removeAdjustmentListener(this)
            value = maximum - extent
            scrollBar.value = value
            scrollBar.addAdjustmentListener(this)
        }

        if (adjustScrollBar && viewportPosition == START) {
            //  Keep the viewport at the same relative viewportPosition
            scrollBar.removeAdjustmentListener(this)
            value = value + maximum - previousMaximum
            scrollBar.value = value
            scrollBar.addAdjustmentListener(this)
        }

        previousValue = value
        previousMaximum = maximum
    }

    companion object {
        private const val HORIZONTAL = 0
        private const val VERTICAL = 1
        private const val START = 0
        private const val END = 1
    }
}
