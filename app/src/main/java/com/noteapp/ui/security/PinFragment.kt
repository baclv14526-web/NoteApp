package com.noteapp.ui.security

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.noteapp.R
import com.noteapp.databinding.FragmentPinBinding
import com.noteapp.utils.SecurityManager

class PinFragment : Fragment() {

    private var _b: FragmentPinBinding? = null
    private val b get() = _b!!

    private lateinit var security: SecurityManager
    private var pinInput = StringBuilder()
    private var isVerifying = false
    private var noteId = -1L
    private var isSettingPin = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentPinBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        security    = SecurityManager(requireContext())
        isVerifying = arguments?.getBoolean("isVerifying", false) ?: false
        noteId      = arguments?.getLong("noteId", -1L) ?: -1L
        isSettingPin = arguments?.getBoolean("isSettingPin", false) ?: false

        setupTitle()
        setupKeypad()
        setupBiometric()
    }

    private fun setupTitle() {
        b.tvTitle.text = when {
            isSettingPin  -> "Tạo mã PIN 6 số"
            isVerifying   -> "Nhập mã PIN"
            else          -> "Nhập mã PIN"
        }
        b.btnBiometric.visibility =
            if (isVerifying && security.isBiometricAvailable(requireContext()))
                View.VISIBLE else View.GONE
    }

    private fun setupKeypad() {
        val numBtns = listOf(
            b.btn0, b.btn1, b.btn2, b.btn3, b.btn4,
            b.btn5, b.btn6, b.btn7, b.btn8, b.btn9
        )
        numBtns.forEachIndexed { idx, btn ->
            btn.setOnClickListener { appendDigit(idx.toString()) }
        }
        b.btnDelete.setOnClickListener { deleteDigit() }
        b.btnDelete.setOnLongClickListener { clearPin(); true }
    }

    private fun setupBiometric() {
        b.btnBiometric.setOnClickListener {
            security.showBiometricPrompt(
                fragment  = this,
                onSuccess = { onAuthSuccess() },
                onFailed  = { Toast.makeText(context, "Xác thực thất bại", Toast.LENGTH_SHORT).show() },
                onError   = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            )
        }
        // Auto-show biometric if verifying
        if (isVerifying && security.isBiometricAvailable(requireContext())) {
            b.btnBiometric.performClick()
        }
    }

    private fun appendDigit(d: String) {
        if (pinInput.length >= 6) return
        pinInput.append(d)
        updateDots()
        if (pinInput.length == 6) validatePin()
    }

    private fun deleteDigit() {
        if (pinInput.isNotEmpty()) {
            pinInput.deleteCharAt(pinInput.lastIndex)
            updateDots()
        }
    }

    private fun clearPin() {
        pinInput.clear()
        updateDots()
    }

    private fun updateDots() {
        val dots = listOf(b.dot1, b.dot2, b.dot3, b.dot4, b.dot5, b.dot6)
        dots.forEachIndexed { i, dot ->
            dot.setBackgroundResource(
                if (i < pinInput.length) R.drawable.dot_filled else R.drawable.dot_empty
            )
        }
    }

    private fun validatePin() {
        val pin = pinInput.toString()
        when {
            isSettingPin -> {
                security.setPin(pin)
                Toast.makeText(context, "Đã tạo mã PIN", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            isVerifying -> {
                if (security.verifyPin(pin)) {
                    onAuthSuccess()
                } else {
                    clearPin()
                    b.tvError.visibility = View.VISIBLE
                    b.tvError.text = "Mã PIN không đúng. Thử lại."
                }
            }
            !security.hasPin() -> {
                // First time: set PIN
                security.setPin(pin)
                Toast.makeText(context, "Đã tạo mã PIN bảo mật", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            else -> {
                if (security.verifyPin(pin)) {
                    onAuthSuccess()
                } else {
                    clearPin()
                    b.tvError.visibility = View.VISIBLE
                    b.tvError.text = "Mã PIN không đúng!"
                }
            }
        }
    }

    private fun onAuthSuccess() {
        b.tvError.visibility = View.GONE
        if (noteId > 0) {
            findNavController().navigate(
                R.id.action_pinFragment_to_editorFragment,
                bundleOf("noteId" to noteId)
            )
        } else {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
