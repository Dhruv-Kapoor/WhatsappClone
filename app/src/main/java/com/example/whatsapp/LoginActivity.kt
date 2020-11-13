package com.example.whatsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.activity_login.*

const val KEY_PHONE_NUMBER = "phone number"

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etNumber.addTextChangedListener {
            btnNext.isEnabled = it?.length == 10
        }

        btnNext.setOnClickListener {
            val number = ccp.selectedCountryCodeWithPlus + etNumber.text
            val dialog = AlertDialog.Builder(this).apply {
                setPositiveButton(
                    "OK"
                ) { _, _ ->
                    startActivity(Intent(this@LoginActivity, OtpActivity::class.java).apply {
                        putExtra(KEY_PHONE_NUMBER, number)
                    })
                    finish()
                }
                setNegativeButton(
                    "Edit"
                ) { dialog, _ -> dialog.dismiss() }
                setMessage("We will be verifying the phone number:$number\nIs this OK, or would you like to edit the number?")
            }.create()
            dialog.show()
        }

    }
}