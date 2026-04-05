package com.imadev.foody.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentLoginBinding
import com.imadev.foody.model.Client
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.Constants.CLIENTS_COLLECTION
import com.imadev.foody.utils.moveTo
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "LoginFragment"

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding, AuthViewModel>() {

    override val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var callbackManager: CallbackManager? = null

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign in failed code: ${e.statusCode}", e)
            Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginBinding =
        FragmentLoginBinding.inflate(layoutInflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        
        setupGoogleSignIn()

        // Auto-fill removed for security and production
        // binding.editTextEmail.setText("admin@foody.com")
        // binding.editTextPassword.setText("admin123")

        binding.googleSignInBtn.setOnClickListener { signIn() }
        
        binding.emailSignInBtn.setOnClickListener { signInWithEmail() }

        binding.btnIgnore.setOnClickListener { 
            moveTo(HomeActivity::class.java) 
        }
        
        callbackManager = CallbackManager.Factory.create()
        binding.facebookSignInBtn.setOnClickListener { signInWithFacebook() }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun signIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun signInWithEmail() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar3.visibility = View.VISIBLE
        
        // Try to sign in
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    checkAndSaveUser(user?.uid, user?.displayName, null, user?.email)
                } else {
                    // If login fails, check if it's because the user doesn't exist (Bootstrap Admin)
                    if (email == "admin@foody.com") {
                        registerAdmin(email, password)
                    } else {
                        binding.progressBar3.visibility = View.GONE
                        Log.e(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(requireContext(), "Auth failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun registerAdmin(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.progressBar3.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Force role to ADMIN for this specific email
                    checkAndSaveUser(user?.uid, "Admin Foody", null, user?.email, forceAdmin = true)
                } else {
                    Toast.makeText(requireContext(), "Admin registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                checkAndSaveUser(user?.uid, user?.displayName, user?.phoneNumber, user?.email)
            } else {
                Toast.makeText(requireContext(), "Auth Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAndSaveUser(uid: String?, username: String?, phone: String?, email: String?, forceAdmin: Boolean = false) {
        if (uid == null) return
        
        val databaseUrl = "https://foody-app-a1b12-default-rtdb.firebaseio.com/"
        val realtimeDb = FirebaseDatabase.getInstance(databaseUrl).reference
        val userRef = realtimeDb.child(CLIENTS_COLLECTION).child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            FirebaseMessaging.getInstance().token.addOnSuccessListener { newToken ->
                val existingRole = snapshot.child("role").value as? String
                val finalRole = if (forceAdmin || email == "admin@foody.com") Client.ROLE_ADMIN else (existingRole ?: Client.ROLE_USER)

                if (!snapshot.exists() || forceAdmin) {
                    val client = Client(id = uid, username = username ?: "User", address = null, phone = phone, email = email, token = newToken, role = finalRole)
                    userRef.setValue(client).addOnSuccessListener {
                        navigateToCorrectDashboard(finalRole)
                    }
                } else {
                    userRef.child("token").setValue(newToken).addOnSuccessListener {
                        navigateToCorrectDashboard(finalRole)
                    }
                }
            }
        }.addOnFailureListener {
            Log.e(TAG, "Database error: ${it.message}")
            moveTo(HomeActivity::class.java)
        }
    }

    private fun navigateToCorrectDashboard(role: String) {
        if (isAdded) {
            val intent = Intent(requireContext(), HomeActivity::class.java).apply {
                putExtra("USER_ROLE", role)
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun signInWithFacebook() {
    }

    override fun setToolbarTitle(activity: HomeActivity) {}
}
