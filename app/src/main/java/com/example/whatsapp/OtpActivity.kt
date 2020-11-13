package com.example.whatsapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_otp.*
import java.util.concurrent.TimeUnit

private const val TAG = "OtpActivity"

class OtpActivity : AppCompatActivity() {

    private var resendCount = 0
    private var verificationId = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }
    private lateinit var counter: CountDownTimer

    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            startActivity(
                Intent(this@OtpActivity, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = ds.linkColor
            ds.isUnderlineText = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        val number = intent.getStringExtra(KEY_PHONE_NUMBER)!!
        tvNumber.text = "Verify $number"

        val waitingText =
            SpannableString("Waiting to automatically detect an SMS sent to\n $number. Wrong Number?")

        waitingText.setSpan(
            clickableSpan,
            waitingText.length - 13,
            waitingText.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvCounter.movementMethod = LinkMovementMethod.getInstance()
        tvCounter.text = waitingText

        sendOtp(number, 30000)

        btnResend.setOnClickListener {
            ++resendCount
            when (resendCount) {
                1 -> {
                    startCounter(45000)
                    resendOtp(number, 450000)
                }
                2 -> {
                    startCounter(60000)
                    resendOtp(number, 60000)
                }
                else -> {
                    startCounter(90000)
                    resendOtp(number, 900000)
                }
            }
            btnResend.isEnabled = false
        }

        etOtp.addTextChangedListener {
            if (it?.length == 6) {
                Log.d(TAG, "onCreate: $verificationId")
                val credential = PhoneAuthProvider.getCredential(verificationId, etOtp.text.toString())
                signInWithPhoneAuthCredentials(credential)
            }
        }
    }

    private fun resendOtp(number: String, timeOut: Long) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                etOtp.setText(credential.smsCode)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                Toast.makeText(
                    this@OtpActivity,
                    "OTP verification failed. Try Again!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@OtpActivity.verificationId = verificationId
                resendToken = token
                Toast.makeText(this@OtpActivity, "OTP Sent Again", Toast.LENGTH_SHORT).show()
            }
        }
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            number,
            timeOut,
            TimeUnit.MILLISECONDS,
            this,
            callbacks,
            resendToken
        )

    }

    private fun sendOtp(number: String, timeOut: Long) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                etOtp.setText(credential.smsCode)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                Toast.makeText(
                    this@OtpActivity,
                    "OTP verification failed. Try Again!",
                    Toast.LENGTH_SHORT
                ).show()
                p0.printStackTrace()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@OtpActivity.verificationId = verificationId
                resendToken = token
                startCounter(30000)
                Toast.makeText(this@OtpActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
            }

            
        }
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            number,
            timeOut,
            TimeUnit.MILLISECONDS,
            this,
            callbacks
        )
    }

    private fun signInWithPhoneAuthCredentials(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                Toast.makeText(this@OtpActivity, "Verification Successful", Toast.LENGTH_SHORT)
                    .show()
                startActivity(
                    Intent(this@OtpActivity, ProfileActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                )
            }
            .addOnFailureListener {
                Toast.makeText(this@OtpActivity, "Verification Failed", Toast.LENGTH_SHORT)
                    .show()
                Log.d(TAG, "signInWithPhoneAuthCredentials: $it")
            }
    }

    private fun startCounter(millisInFuture: Long) {
        counter = object : CountDownTimer(millisInFuture, 1000) {
            override fun onFinish() {
                btnResend.isEnabled = true
                btnResend.text = "Resend"
            }

            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                btnResend.text = "Resend in $secondsLeft seconds"
            }

        }
        counter.start()
    }

    override fun onDestroy() {
        if(::counter.isInitialized){
            counter.cancel()
        }
        super.onDestroy()
    }

    override fun onBackPressed() {}
}

