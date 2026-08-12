package com.naamtaan1008.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import com.naamtaan1008.app.data.TokenStorage
import com.naamtaan1008.app.data.model.CommunityUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 社区 tab：登录/注册/邮箱验证 + 资料（位置五档/动态定位）。
 * 原生实现，直连社区 API（community 前缀）。
 */
class CommunityFragment : Fragment() {

    private lateinit var authBox: View
    private lateinit var profileBox: View
    private lateinit var tabs: TabLayout
    private lateinit var loginBox: View
    private lateinit var registerBox: View
    private lateinit var verifyBox: View

    private var pendingEmail = ""
    private var pendingNickname = ""
    private var pendingPassword = ""

    private val levelOptions = listOf(
        "L1" to "城市级（仅显示城市）",
        "L2" to "区县级（登录用户可见区县）",
        "L3" to "精确公开（登录用户可见精确位置）",
        "L4" to "精确仅互关（精确位置仅互相关注的人可见）",
        "L5" to "完全不共享（任何人不显示位置）"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_community, container, false)
        authBox = v.findViewById(R.id.communityAuthBox)
        profileBox = v.findViewById(R.id.communityProfileBox)
        tabs = v.findViewById(R.id.communityTabs)
        loginBox = v.findViewById(R.id.loginBox)
        registerBox = v.findViewById(R.id.registerBox)
        verifyBox = v.findViewById(R.id.verifyBox)

        setupAuthTabs()
        setupLogin()
        setupRegister()
        setupVerify()

        // 启动时按登录态渲染
        if (TokenStorage.isLoggedIn()) {
            loadAndRenderProfile()
        }

        // 加载未来 7 天演出（公开）—— 地图社区的核心数据
        loadUpcomingShows()

        return v
    }

    // ==================== 未来7天演出（地图社区数据） ====================
    private fun loadUpcomingShows() {
        lifecycleScope.launch {
            val container = viewUpcomingContainer()
            val fb = viewUpcomingFeedback()
            container.removeAllViews()
            val overview = withContext(Dispatchers.IO) { Repository.fetchMapOverview() }
            val venuePoints = overview.venues.filter { it.shows.isNotEmpty() }

            if (venuePoints.isEmpty()) {
                fb.text = "暂无可展示的演出"
                fb.visibility = View.VISIBLE
                return@launch
            }
            fb.visibility = View.GONE

            venuePoints.take(15).forEach { vp ->
                container.addView(buildVenueBlock(vp))
            }
        }
    }

    private fun buildVenueBlock(vp: com.naamtaan1008.app.data.model.VenuePoint): View {
        val ctx = requireContext()
        val block = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.item_background))
        }

        // 场馆名 + 城市
        val header = android.widget.TextView(ctx).apply {
            text = vp.name + (if (vp.city.isNotBlank()) " · ${vp.city}" else "")
            textSize = 15f
            setTextColor(ContextCompat.getColor(ctx, R.color.zakka_ink))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        block.addView(header)

        // 该场馆的演出列表（最多 5 条）
        vp.shows.take(5).forEach { s ->
            val line = android.widget.TextView(ctx).apply {
                text = "· ${showTimeShort(s.showTime)}  ${s.title}" +
                        (if (s.price.isNotBlank()) "（${s.price}）" else "")
                textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.zakka_brown))
                setPadding(0, dp(4), 0, 0)
            }
            block.addView(line)
        }
        if (vp.shows.size > 5) {
            block.addView(android.widget.TextView(ctx).apply {
                text = "… 还有 ${vp.shows.size - 5} 场"
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.zakka_brown))
            })
        }
        return block
    }

    private fun showTimeShort(t: String): String {
        val m = Regex("(\\d{4})[\\/\\-](\\d{1,2})[\\/\\-](\\d{1,2})").find(t) ?: return t
        return "${m.groupValues[2]}月${m.groupValues[3]}日"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ==================== 标签切换 ====================
    private fun setupAuthTabs() {
        tabs.addTab(tabs.newTab().setText("登录"))
        tabs.addTab(tabs.newTab().setText("注册"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val reg = tab.position == 1
                loginBox.visibility = if (reg) View.GONE else View.VISIBLE
                registerBox.visibility = if (reg) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // ==================== 反馈 ====================
    private fun feedback(tv: TextView, msg: String?, isError: Boolean) {
        tv.text = msg ?: ""
        tv.visibility = if (msg.isNullOrBlank()) View.GONE else View.VISIBLE
        tv.setTextColor(ContextCompat.getColor(requireContext(),
            if (isError) R.color.zakka_clay else R.color.zakka_brown))
    }

    // ==================== 登录 ====================
    private fun setupLogin() {
        val email = viewLoginEmail()
        val password = viewLoginPassword()
        viewLoginBtn().setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString()
            if (e.isEmpty() || p.isEmpty()) {
                feedback(viewLoginFeedback(), "请填写邮箱和密码", true); return@setOnClickListener
            }
            viewLoginBtn().isEnabled = false
            viewLoginBtn().text = "登录中…"
            lifecycleScope.launch {
                val resp = withContext(Dispatchers.IO) { Repository.communityLogin(e, p) }
                viewLoginBtn().isEnabled = true
                viewLoginBtn().text = "登录"
                if (resp.success) {
                    TokenStorage.save(resp.token, resp.user.email)
                    feedback(viewLoginFeedback(), "登录成功", false)
                    loadAndRenderProfile()
                } else {
                    feedback(viewLoginFeedback(), resp.error ?: "登录失败", true)
                }
            }
        }
    }

    // ==================== 注册 ====================
    private fun setupRegister() {
        viewRegBtn().setOnClickListener {
            val email = viewRegEmail().text.toString().trim()
            val nickname = viewRegNickname().text.toString().trim()
            val p1 = viewRegPassword().text.toString()
            val p2 = viewRegPassword2().text.toString()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                feedback(viewRegFeedback(), "邮箱格式不正确", true); return@setOnClickListener
            }
            if (nickname.isEmpty()) {
                feedback(viewRegFeedback(), "请填写昵称", true); return@setOnClickListener
            }
            if (p1.length < 8) {
                feedback(viewRegFeedback(), "密码至少 8 个字符", true); return@setOnClickListener
            }
            if (p1 != p2) {
                feedback(viewRegFeedback(), "两次密码不一致", true); return@setOnClickListener
            }
            viewRegBtn().isEnabled = false
            viewRegBtn().text = "发送中…"
            lifecycleScope.launch {
                val resp = withContext(Dispatchers.IO) {
                    Repository.communityRegister(email, p1, nickname)
                }
                viewRegBtn().isEnabled = true
                viewRegBtn().text = "发送验证码"
                if (resp.success) {
                    pendingEmail = email
                    pendingNickname = nickname
                    pendingPassword = p1
                    showVerifyStep(resp.message ?: "验证码已发送至邮箱")
                } else {
                    feedback(viewRegFeedback(), resp.error ?: "注册失败", true)
                }
            }
        }
    }

    // ==================== 验证 ====================
    private fun setupVerify() {
        viewVerifyBtn().setOnClickListener {
            val code = viewVerifyCode().text.toString().trim()
            if (code.length != 6) {
                feedback(viewVerifyFeedback(), "请输入 6 位验证码", true); return@setOnClickListener
            }
            viewVerifyBtn().isEnabled = false
            viewVerifyBtn().text = "验证中…"
            lifecycleScope.launch {
                val resp = withContext(Dispatchers.IO) {
                    Repository.communityVerify(pendingEmail, code, pendingPassword, pendingNickname)
                }
                viewVerifyBtn().isEnabled = true
                viewVerifyBtn().text = "验证并激活"
                if (resp.success) {
                    feedback(viewVerifyFeedback(), "验证成功，请登录", false)
                    // 回到登录 tab
                    withContext(Dispatchers.Main) {
                        tabs.getTabAt(0)?.select()
                        verifyBox.visibility = View.GONE
                        viewLoginEmail().setText(pendingEmail)
                    }
                } else {
                    feedback(viewVerifyFeedback(), resp.error ?: "验证失败", true)
                }
            }
        }
    }

    private fun showVerifyStep(hint: String) {
        loginBox.visibility = View.GONE
        registerBox.visibility = View.GONE
        verifyBox.visibility = View.VISIBLE
        viewVerifyHint().text = hint
    }

    // ==================== 资料 ====================
    private fun loadAndRenderProfile() {
        val token = TokenStorage.token()
        lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) { Repository.communityMe(token) }
            if (resp.success) {
                renderProfile(resp.user)
            } else {
                // token 失效
                TokenStorage.clear()
                Toast.makeText(requireContext(), "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                authBox.visibility = View.VISIBLE
                profileBox.visibility = View.GONE
            }
        }
    }

    private fun renderProfile(user: CommunityUser) {
        authBox.visibility = View.GONE
        profileBox.visibility = View.VISIBLE

        viewProfileAvatar().text = (user.nickname.ifBlank { "?" }).take(1)
        viewProfileName().text = user.nickname
        viewProfileEmail().text = user.email
        viewPfNickname().setText(user.nickname)
        viewPfBio().setText(user.bio)
        viewPfCity().setText(user.city)
        viewPfDistrict().setText(user.district)
        viewPfRoles().setText(user.roles.joinToString("、"))
        viewPfRefresh().isChecked = user.refreshLocation

        // 动态填充位置五档 radio
        val group = viewPfLevelGroup()
        group.removeAllViews()
        levelOptions.forEach { (value, desc) ->
            val rb = RadioButton(requireContext())
            rb.text = "$desc"
            rb.tag = value
            rb.isChecked = (user.locationLevel == value)
            group.addView(rb)
        }

        setupProfileActions()
    }

    private fun setupProfileActions() {
        viewProfileSaveBtn().setOnClickListener {
            val token = TokenStorage.token()
            val nickname = viewPfNickname().text.toString().trim()
            val bio = viewPfBio().text.toString().trim()
            val city = viewPfCity().text.toString().trim()
            val district = viewPfDistrict().text.toString().trim()
            val roles = viewPfRoles().text.toString().split('、', '，', ',')
                .map { it.trim() }.filter { it.isNotEmpty() }
            val level = selectedLevel()
            val refresh = viewPfRefresh().isChecked

            if (nickname.isEmpty()) {
                feedback(viewProfileFeedback(), "昵称不能为空", true); return@setOnClickListener
            }

            viewProfileSaveBtn().isEnabled = false
            viewProfileSaveBtn().text = "保存中…"
            lifecycleScope.launch {
                val resp = withContext(Dispatchers.IO) {
                    Repository.communityUpdateProfile(token, nickname, bio, city, district, roles, level, refresh)
                }
                viewProfileSaveBtn().isEnabled = true
                viewProfileSaveBtn().text = "保存资料"
                if (resp.success) {
                    feedback(viewProfileFeedback(), "已保存", false)
                    renderProfile(resp.user)
                } else {
                    feedback(viewProfileFeedback(), resp.error ?: "保存失败", true)
                }
            }
        }

        // 定位上报
        viewReportLocationBtn().setOnClickListener {
            if (!TokenStorage.isLoggedIn()) return@setOnClickListener
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    1001
                )
                return@setOnClickListener
            }
            doReportLocation()
        }

        viewLogoutBtn().setOnClickListener {
            TokenStorage.clear()
            authBox.visibility = View.VISIBLE
            profileBox.visibility = View.GONE
            Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectedLevel(): String {
        val group = viewPfLevelGroup()
        val checked = group.findViewById<RadioButton>(group.checkedRadioButtonId)
        return (checked?.tag as? String) ?: "L1"
    }

    private fun doReportLocation() {
        val token = TokenStorage.token()
        try {
            val lm = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE)
                as android.location.LocationManager
            val provider = if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
                android.location.LocationManager.GPS_PROVIDER
            else android.location.LocationManager.NETWORK_PROVIDER
            @Suppress("MissingPermission")
            val loc = lm.getLastKnownLocation(provider)
            if (loc == null) {
                feedback(viewProfileFeedback(), "暂无法获取位置，请确认已开启定位", true)
                return
            }
            viewReportLocationBtn().isEnabled = false
            viewReportLocationBtn().text = "上报中…"
            lifecycleScope.launch {
                val resp = withContext(Dispatchers.IO) {
                    Repository.communityReportLocation(token, loc.latitude, loc.longitude)
                }
                viewReportLocationBtn().isEnabled = true
                viewReportLocationBtn().text = "上报当前位置"
                if (resp.success) {
                    feedback(viewProfileFeedback(), "位置已上报", false)
                } else {
                    feedback(viewProfileFeedback(), resp.error ?: "上报失败（可能未开启动态刷新开关）", true)
                }
            }
        } catch (e: Exception) {
            feedback(viewProfileFeedback(), "定位失败：${e.message}", true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doReportLocation()
        } else {
            Toast.makeText(requireContext(), "需要定位权限才能上报位置", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== findViewById 辅助 ====================
    private fun viewUpcomingContainer(): android.widget.LinearLayout = requireView().findViewById(R.id.upcomingContainer)
    private fun viewUpcomingFeedback(): TextView = requireView().findViewById(R.id.upcomingFeedback)
    private fun viewLoginEmail(): EditText = requireView().findViewById(R.id.loginEmail)
    private fun viewLoginPassword(): EditText = requireView().findViewById(R.id.loginPassword)
    private fun viewLoginBtn(): Button = requireView().findViewById(R.id.loginBtn)
    private fun viewLoginFeedback(): TextView = requireView().findViewById(R.id.loginFeedback)
    private fun viewRegEmail(): EditText = requireView().findViewById(R.id.regEmail)
    private fun viewRegNickname(): EditText = requireView().findViewById(R.id.regNickname)
    private fun viewRegPassword(): EditText = requireView().findViewById(R.id.regPassword)
    private fun viewRegPassword2(): EditText = requireView().findViewById(R.id.regPassword2)
    private fun viewRegBtn(): Button = requireView().findViewById(R.id.regBtn)
    private fun viewRegFeedback(): TextView = requireView().findViewById(R.id.regFeedback)
    private fun viewVerifyHint(): TextView = requireView().findViewById(R.id.verifyHint)
    private fun viewVerifyCode(): EditText = requireView().findViewById(R.id.verifyCode)
    private fun viewVerifyBtn(): Button = requireView().findViewById(R.id.verifyBtn)
    private fun viewVerifyFeedback(): TextView = requireView().findViewById(R.id.verifyFeedback)
    private fun viewProfileAvatar(): TextView = requireView().findViewById(R.id.profileAvatar)
    private fun viewProfileName(): TextView = requireView().findViewById(R.id.profileName)
    private fun viewProfileEmail(): TextView = requireView().findViewById(R.id.profileEmail)
    private fun viewPfNickname(): EditText = requireView().findViewById(R.id.pfNickname)
    private fun viewPfBio(): EditText = requireView().findViewById(R.id.pfBio)
    private fun viewPfCity(): EditText = requireView().findViewById(R.id.pfCity)
    private fun viewPfDistrict(): EditText = requireView().findViewById(R.id.pfDistrict)
    private fun viewPfRoles(): EditText = requireView().findViewById(R.id.pfRoles)
    private fun viewPfLevelGroup(): RadioGroup = requireView().findViewById(R.id.pfLevelGroup)
    private fun viewPfRefresh(): android.widget.CheckBox = requireView().findViewById(R.id.pfRefresh)
    private fun viewProfileSaveBtn(): Button = requireView().findViewById(R.id.profileSaveBtn)
    private fun viewReportLocationBtn(): Button = requireView().findViewById(R.id.reportLocationBtn)
    private fun viewProfileFeedback(): TextView = requireView().findViewById(R.id.profileFeedback)
    private fun viewLogoutBtn(): Button = requireView().findViewById(R.id.logoutBtn)
}
