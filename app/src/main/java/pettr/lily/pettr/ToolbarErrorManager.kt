package pettr.lily.pettr

import android.widget.TextView

/**
 * Created by Lily Hoskin on 24/03/2018.
 * Used to manage priority of error messages to show to user
 */
class ToolbarErrorManager(private val textView: TextView) {



    enum class ToolbarErrorTypes {
        ServerError, NetworkError, LocationError, NoError

    }

    data class ToolbarError(val tag: ToolbarErrorTypes, val errorMessage: String) {
    }

    fun updateError(toolbarError: ToolbarError) {
        textView.text = toolbarError.errorMessage
    }

}