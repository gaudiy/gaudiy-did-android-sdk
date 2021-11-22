package com.example.gaudiy_did_android_sdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gaudiy.gaudiy_did.GaudiySignup

class FirstFragment : Fragment() {
    private lateinit var didButton:Button

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        didButton = view.findViewById<Button>(R.id.button_first);
        didButton.setOnClickListener {
            Toast.makeText(context, "DID認証画面を開く", Toast.LENGTH_SHORT).show()

            // SDK呼び出し
            val sdk = GaudiySignup()
            this.activity?.let { it1 ->
                sdk.setController(it1)
                sdk.asyncExecute("ak_3ch89dsedpa55532", "serviceUserId_android_${Math.random()}", "sample-gaudiy-app://verification", "https://sample-auth-gateway.vercel.app/top")
            }
//            GaudiySignup().asyncExecute("ak_3ch89dsedpa55532", "serviceUserId_android_${Math.random()}", "sample-gaudiy-app://verification", "http://192.168.0.115:3001/top")
        }
    }
}
