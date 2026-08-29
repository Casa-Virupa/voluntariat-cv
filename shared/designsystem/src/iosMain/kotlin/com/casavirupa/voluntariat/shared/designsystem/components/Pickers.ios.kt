package com.casavirupa.voluntariat.shared.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.UIKit.UICalendarSelectionSingleDate
import platform.UIKit.UICalendarSelectionSingleDateDelegateProtocol
import platform.UIKit.UICalendarView
import platform.UIKit.UICalendarViewDelegateProtocol
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle
import platform.UIKit.UIDevice
import platform.UIKit.UIFontDescriptorSystemDesignRounded
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun NativeDatePicker(
    date: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate?,
) {
    val coordinator = remember(minDate) {
        CalendarCoordinator(
            onDateChange = onDateSelected,
            minDate = minDate,
        )
    }
    val selectionBehavior = remember(coordinator) {
        UICalendarSelectionSingleDate(delegate = coordinator)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
        ) {
            UIKitView(
                factory = {
                    UICalendarView().apply {
                        calendar =
                            NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply {
                                firstWeekday = 2u // 1 is Sunday, 2 is Monday
                            }
                        fontDesign = UIFontDescriptorSystemDesignRounded
                        delegate = coordinator
                        this.selectionBehavior = selectionBehavior
                        backgroundColor = UIColor.whiteColor
                        overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    }
                },
                properties = UIKitInteropProperties(
                    interactionMode = UIKitInteropInteractionMode.NonCooperative,
                    placedAsOverlay = true,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                update = { view ->
                    val selection = view.selectionBehavior as? UICalendarSelectionSingleDate

                    if (date != null) {
                        selection?.setSelectedDate(date.toNSDateComponents(), animated = true)
                    }
                },
            )
        }
    }
}

class CalendarCoordinator(
    private val onDateChange: (LocalDate?) -> Unit,
    private val minDate: LocalDate? = null,
) : NSObject(),
    UICalendarViewDelegateProtocol,
    UICalendarSelectionSingleDateDelegateProtocol {
    @ObjCSignatureOverride
    override fun dateSelection(
        selection: UICalendarSelectionSingleDate,
        didSelectDate: NSDateComponents?,
    ) {
        onDateChange(didSelectDate?.toLocalDate())
    }

    @ObjCSignatureOverride
    override fun dateSelection(
        selection: UICalendarSelectionSingleDate,
        canSelectDate: NSDateComponents?,
    ): Boolean {
        val date = canSelectDate?.toLocalDate() ?: return false
        return minDate == null || date >= minDate
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun NativeTimePicker(
    initialTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val coordinator = remember {
        TimePickerCoordinator(onTimeChange = onTimeSelected)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
            color = Color.White,
            shadowElevation = 6.dp,
        ) {
            UIKitView(
                factory = {
                    UIDatePicker().apply {
                        datePickerMode = UIDatePickerMode.UIDatePickerModeTime
                        preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
                        backgroundColor = UIColor.whiteColor
                        overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight

                        val initialOrNow = initialTime ?: currentTimeAsLocalTime()
                        date = NSCalendar
                            .currentCalendar
                            .dateFromComponents(initialOrNow.toNSDateComponents())
                            ?: NSDate()

                        addTarget(
                            target = coordinator,
                            action = platform.Foundation.NSSelectorFromString("valueChanged:"),
                            forControlEvents = UIControlEventValueChanged,
                        )
                    }
                },
                properties = UIKitInteropProperties(
                    interactionMode = UIKitInteropInteractionMode.Cooperative(),
                    placedAsOverlay = true
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp),
                update = { },
            )
        }
    }
}

class TimePickerCoordinator(private val onTimeChange: (LocalTime) -> Unit) : NSObject() {
    @ObjCAction
    fun valueChanged(sender: UIDatePicker) {
        val calendar = NSCalendar.currentCalendar
        val components = calendar.components(
            NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = sender.date,
        )
        onTimeChange(components.toLocalTime())
    }
}


private fun NSDateComponents.toLocalDate(): LocalDate? = if (year > 0 && month > 0 && day > 0) {
    LocalDate(year.toInt(), month.toInt(), day.toInt())
} else {
    null
}

private fun LocalTime.toNSDateComponents(): NSDateComponents = NSDateComponents().apply {
    hour = this@toNSDateComponents.hour.toLong()
    minute = this@toNSDateComponents.minute.toLong()
}

private fun NSDateComponents.toLocalTime(): LocalTime = LocalTime(
    hour.toInt(),
    minute.toInt(),
)

private fun currentTimeAsLocalTime(): LocalTime {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(
        NSCalendarUnitHour or NSCalendarUnitMinute,
        fromDate = NSDate(),
    )
    return components.toLocalTime()
}
