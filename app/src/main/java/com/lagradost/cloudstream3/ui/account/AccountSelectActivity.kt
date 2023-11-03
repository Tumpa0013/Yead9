package com.lagradost.cloudstream3.ui.account

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.CommonActivity.loadThemes
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.ActivityAccountSelectBinding
import com.lagradost.cloudstream3.ui.account.AccountAdapter.Companion.VIEW_TYPE_EDIT_ACCOUNT
import com.lagradost.cloudstream3.ui.account.AccountHelper.showPinInputDialog
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.isTvSettings
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.getAccounts
import com.lagradost.cloudstream3.utils.UIHelper.colorFromAttribute

class AccountSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val accounts = getAccounts(this@AccountSelectActivity)
        
        // Don't show account selection if there is only
        // one account that exists
        if (accounts.count() <= 1) {
            navigateToMainActivity()
            return
        }

        CommonActivity.init(this)
        loadThemes(this)

        window.navigationBarColor = colorFromAttribute(R.attr.primaryBlackBackground)

        val binding = ActivityAccountSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerView: RecyclerView = binding.accountRecyclerView

        val adapter = AccountAdapter(
            accounts,
            // Handle the selected account
            accountSelectCallback = { onAccountSelected(it) },
            accountCreateCallback = { onAccountUpdated(it) },
            accountEditCallback = { onAccountUpdated(it) }
        )

        recyclerView.adapter = adapter

        binding.editAccountButton.setOnClickListener {
            adapter.viewType = VIEW_TYPE_EDIT_ACCOUNT
            adapter.notifyDataSetChanged()
        }

        if (isTvSettings()) {
            val spanSize = if (accounts.count() <= 6) {
                accounts.count()
            } else 6

            recyclerView.layoutManager = GridLayoutManager(this, spanSize)
        }
    }

    private fun onAccountSelected(selectedAccount: DataStoreHelper.Account) {
        if (selectedAccount.lockPin != null) {
            // The selected account has a PIN set, prompt the user to enter the PIN
            showPinInputDialog(this@AccountSelectActivity, selectedAccount.lockPin, false) { pin ->
                if (pin == null) return@showPinInputDialog
                // Pin is correct, proceed to main activity
                setAccount(selectedAccount)
                navigateToMainActivity()
            }
        } else {
            // No PIN set for the selected account, proceed to main activity
            setAccount(selectedAccount)
            navigateToMainActivity()
        }
    }

    private fun onAccountUpdated(account: DataStoreHelper.Account) {
        val currentAccounts = DataStoreHelper.accounts.toMutableList()

        val overrideIndex = currentAccounts.indexOfFirst { it.keyIndex == account.keyIndex }
        if (overrideIndex != -1) {
            currentAccounts[overrideIndex] = account
        } else currentAccounts.add(account)

        val currentHomePage = DataStoreHelper.currentHomePage
        setAccount(account)

        DataStoreHelper.accounts = currentAccounts.toTypedArray()
        DataStoreHelper.currentHomePage = currentHomePage

        this.recreate()
    }

    private fun setAccount(account: DataStoreHelper.Account) {
        // Don't reload if it is the same account
        if (DataStoreHelper.selectedKeyIndex == account.keyIndex) {
            return
        }

        DataStoreHelper.selectedKeyIndex = account.keyIndex

        MainActivity.bookmarksUpdatedEvent(true)
        MainActivity.reloadHomeEvent(true)
    }

    private fun navigateToMainActivity() {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        finish() // Finish the account selection activity
    }
}