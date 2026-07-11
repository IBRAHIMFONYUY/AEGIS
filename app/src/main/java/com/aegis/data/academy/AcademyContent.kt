package com.aegis.data.academy

object AcademyContent {
    val topics = listOf(
        AcademyTopic("phishing", "Phishing & Deception", "Spot fake links and emails.", "🎣"),
        AcademyTopic("scams", "Scams & Fraud", "Protect your money from manipulators.", "💰"),
        AcademyTopic("privacy", "Digital Privacy", "Keep your personal data secret.", "🛡"),
        AcademyTopic("cyberbullying", "Cyber-Safety", "Handle online harassment effectively.", "🤝"),
        AcademyTopic("malware", "Malware & APKs", "Secure your device from bad apps.", "🦠")
    )

    fun generateScenarios(): List<AcademyScenario> {
        val scenarios = mutableListOf<AcademyScenario>()
        topics.forEach { topic ->
            AcademyLevel.entries.forEach { level ->
                for (i in 1..50) {
                    scenarios.add(createScenario(topic.id, level, i))
                }
            }
        }
        return scenarios
    }

    private fun createScenario(topicId: String, level: AcademyLevel, index: Int): AcademyScenario {
        return when (topicId) {
            "phishing" -> createPhishingScenario(level, index)
            "scams" -> createScamScenario(level, index)
            "privacy" -> createPrivacyScenario(level, index)
            "cyberbullying" -> createSafetyScenario(level, index)
            "malware" -> createMalwareScenario(level, index)
            else -> createPhishingScenario(level, index)
        }
    }

    private fun createPhishingScenario(level: AcademyLevel, index: Int): AcademyScenario {
        val questions = if (level == AcademyLevel.ADVANCED) {
            listOf(
                "You receive an email with a valid DKIM signature from 'support@yourbank.com', but the 'Reply-To' header points to a different domain. The email asks you to click a link to 're-validate your 2FA token' because of a 'cryptographic roll-over'.",
                "A LinkedIn contact sends you a link to a 'GitHub gist' that they claim contains a fix for a Zero-Day vulnerability in an app you use. The gist contains a 'one-liner' bash command that pipes a URL to 'sh'.",
                "You are auditing your browser extensions and find one called 'Secure Tab' that has permission to 'commuicate with native applications' and 'modify web requests' for all URLs.",
                "Someone on a tech forum claims they have a 'PoC' for an exploit and provides a link to a password-protected ZIP file. They say the password is 'infected' so your AV doesn't flag the PoC.",
                "You receive a push notification from an unknown app that looks exactly like a system 'Find My Device' alert, asking you to 'Sign in to confirm location' via a web view."
            )
        } else {
            listOf(
                "You get an SMS: 'Your Amazon account is locked. Verify here: amazon-security-check.xyz'.",
                "Email from 'IRS': 'You have a pending tax refund of $500. Click to claim: gov-refund-portal.net'.",
                "WhatsApp message: 'I found a video of you online! Look: bit.ly/is-this-you-99'.",
                "Pop-up: 'Your Android is infected with 13 viruses! Download CleanMaster Pro to fix now.'",
                "LinkedIn message from a 'Recruiter': 'Great profile! Check the job description in this .zip file.'",
                "Email from 'Apple': 'Your iCloud storage is full. Upgrade for free at icloud-verify-free.com'.",
                "Facebook message from a friend: 'Hey, I'm stuck in London and lost my wallet. Can you send $200 via PayPal?'",
                "Email from 'HR': 'Updated payroll policy. Please sign in here to view: company-portal-login.com'.",
                "Text from 'Bank': 'Suspicious transaction of $999 detected. If not you, click here to cancel: mybank-alert.co'.",
                "Instagram DM: 'Congratulations! You won our giveaway. DM us your email to claim your prize.'",
                "Email from 'Netflix': 'Payment failed. Update your card details at netflx-billing-update.com'.",
                "Browser alert: 'Security certificate expired. Click here to install the root update.'",
                "Discord message: 'Someone is talking bad about you in this server. Join to see: discord-leaks.xyz'.",
                "Email from 'Microsoft': 'Unusual login from Russia. Verify your identity at microsoft-auth-secure.net'.",
                "SMS: 'Package delivery failed. Pay $1 re-delivery fee here: dhl-track-post.com'.",
                "Email from 'University': 'Your scholarship application requires a signature. Open attached PDF.'",
                "Pop-up on a movie site: 'To watch this video, you must update your Flash Player.'",
                "WhatsApp: 'Happy 50th Anniversary! Coca-Cola is giving away 1000 free fridges. Claim: coca-promo.org'.",
                "Email from 'Boss': 'I'm in a meeting. Can you buy 5 Amazon gift cards and send me the codes ASAP?'",
                "LinkedIn: 'Connect with me to see who viewed your profile.' Followed by a suspicious link."
            )
        }
        
        val qIndex = (index - 1) % questions.size
        val levelModifier = when(level) {
            AcademyLevel.BEGINNER -> " (Easy)"
            AcademyLevel.INTERMEDIATE -> " (Moderate)"
            AcademyLevel.ADVANCED -> " (Expert Technical)"
        }
        return AcademyScenario(
            id = "phishing_${level}_$index",
            topicId = "phishing",
            level = level,
            question = "Scenario $index$levelModifier: ${questions[qIndex]}",
            options = if (level == AcademyLevel.ADVANCED) {
                listOf("Trust DKIM and proceed", "Inspect headers and block", "Verify via bank's official app", "Report to IT/Security")
            } else {
                listOf("Click and follow instructions", "Ignore and delete", "Reply to verify", "Forward to a friend")
            },
            correctOptionIndex = if (level == AcademyLevel.ADVANCED) 2 else 1,
            explanation = if (level == AcademyLevel.ADVANCED) {
                "Advanced phishing can spoof signatures or use valid infrastructure. Multi-channel verification (official app) and header inspection (Reply-To mismatch) are critical for experts."
            } else {
                "Phishing attempts often use urgency (locked account), authority (IRS/Bank), or curiosity (video of you). Always check if the URL matches the official brand exactly and never download unexpected attachments."
            }
        )
    }

    private fun createScamScenario(level: AcademyLevel, index: Int): AcademyScenario {
        val scenarios = listOf(
            "A stranger on Telegram promises 50% daily returns on 'Crypto Mining'. Just send 100 USDT to start.",
            "You win a lottery you never entered. To claim the $1,000,000 prize, you must pay a $5,000 'processing fee'.",
            "Someone calls claiming to be the Police. They say your ID was used in a crime and you must pay a 'bond' in Bitcoin.",
            "A romantic interest you met online asks for $500 for a plane ticket to come visit you for the first time.",
            "An 'influencer' on Twitter says they are giving away free ETH. Just send 0.1 ETH to 'verify' your wallet.",
            "You receive a 'Mystery Box' offer for $1, but the fine print signs you up for a $99/month subscription.",
            "A 'Microsoft Support' technician calls saying they detected a virus on your PC and need remote access.",
            "Someone offers you a high-paying job ($5000/week) for 'reshipping packages' from your home.",
            "A 'Cousin' on WhatsApp says they're in an accident and need 5000 MOMO immediately. 'Don't tell mom!'.",
            "You find a website selling the new iPhone for 90% off. It only accepts Payment via Gift Cards.",
            "Someone sends you a 'wrong number' text, then starts a friendly conversation about their wealthy uncle's crypto tips.",
            "A pop-up says you are the 1,000,000th visitor. Click to claim your free iPad!",
            "You get an email about an 'Unclaimed Inheritance' from a distant relative in another country.",
            "An app promises to show you 'who unfollowed you' if you provide your Instagram username and password.",
            "Someone offers to 'double your money' in 24 hours using a 'secret banking loophole'.",
            "A 'Recruiter' asks you to pay $50 for a 'background check' before your interview.",
            "A caller says they are from your utility company and will cut your power in 30 minutes if you don't pay via a link.",
            "Someone on a dating app asks you to move the conversation to 'WhatsApp' or 'Telegram' immediately.",
            "A 'financial advisor' recommends a 'guaranteed' investment in a company that doesn't have a website.",
            "You get a DM saying 'I made $10,000 this week from home. Ask me how!' with a screenshot of a fake bank balance.",
            "Text from 'Amazon': 'Thank you for your purchase of a Sony TV ($1200). If this wasn't you, call this number.'",
            "WhatsApp: 'Hey, I'm your old friend from high school. My phone was stolen. Can you send me $100 for a taxi?'",
            "Email: 'Government Payout Notice'. You are entitled to a pandemic relief check. Enter info: relief-claim.com.",
            "Someone DM's you: 'I accidentally reported your Steam account. To fix it, talk to this moderator on Discord.'",
            "A website offers 'Free Robux' if you complete a survey and enter your account login.",
            "You get a call from 'Social Security Administration' saying your SSN is about to be cancelled.",
            "WhatsApp: 'Your child is at the police station. Pay the fine now via MOMO to release them.'",
            "Facebook: 'Get a government-backed small business loan today. No credit check. WhatsApp us.'",
            "Email: 'Your Netflix account will be suspended in 24 hours. Update billing info now.'",
            "A stranger on Instagram asks for a 'favor': 'I can't receive my commission, can I send it to your PayPal?'",
            "You find a 'Work From Home' ad that requires you to buy a $500 'starter kit' of office supplies from them.",
            "SMS: 'USPS: Your package is held at our warehouse. Pay the $1.99 redelivery fee here.'",
            "A 'Charity' calls asking for donations for a recent natural disaster, but they only take wire transfers.",
            "Someone on a dating site says: 'I'm a soldier in a foreign country and I found a chest of gold.'",
            "Email: 'Final Warning! Your account will be deleted for violating Terms of Service. Appeal: service-terms.io.'",
            "WhatsApp: 'Happy Birthday! Get a free cake from KFC. Click here: kfc-birthday.xyz.'",
            "Someone sends you a screenshot of a 'winning bet' and asks for a small tip to reveal their strategy.",
            "You get a text with a link to 'Track your lost iPhone' after your phone was actually stolen earlier.",
            "A popup says: 'Your IP address has been flagged for illegal activity. Pay the fine to avoid arrest.'",
            "Someone asks you to 'test' their new game by downloading a .rar file from their Google Drive.",
            "SMS: 'Your bank account has a pending transfer of $4500. Not you? Secure account: mybank-auth.net.'",
            "Email: 'Exclusive invite to the billionaire's crypto mastermind group. Entrance fee: 0.05 BTC.'",
            "A 'landlord' on Facebook asks for a deposit to secure an apartment before you've seen it.",
            "WhatsApp: 'New Message from WhatsApp Admin: Verify your account now or it will be deactivated.'",
            "Someone offers to buy your old laptop for double the price if you ship it to their 'nephew' in another country.",
            "Email: 'System Error: Your payroll was double-paid. Please return the extra $2000 via wire transfer.'",
            "A stranger asks you to 'Like and Subscribe' to a channel for 10 USDT per task. Then asks for a 'VIP fee'.",
            "SMS: 'Verification Code: 9982. If you didn't request this, reply STOP to block your account.'",
            "Someone on Twitter says: 'I'm quitting crypto. Sending 0.5 ETH to the first 50 people who retweet and DM me.'",
            "Email: 'Urgent Action Required: Your IRS tax return was rejected. View the errors in the attached Excel file.'",
            "WhatsApp: 'You won a $1000 Shein gift card! Just pay $5 shipping here.'"
        )
        
        val sIndex = (index - 1) % scenarios.size
        return AcademyScenario(
            id = "scams_${level}_$index",
            topicId = "scams",
            level = level,
            question = "Scenario $index ($level): ${scenarios[sIndex]}",
            options = listOf("Proceed with the offer", "Ask for more details", "Research the company", "Block and ignore"),
            correctOptionIndex = 3,
            explanation = "If it sounds too good to be true, it is. Scammers use 'guaranteed returns', 'immediate threats', and 'secrecy' to manipulate victims. The safest path is to cut contact immediately."
        )
    }
    
    private fun createPrivacyScenario(level: AcademyLevel, index: Int): AcademyScenario {
        val prompts = listOf(
            "A simple 'Calculator' app asks for your GPS Location and Microphone permissions.",
            "A 'Free Wallpaper' app requests access to your Contacts and SMS history.",
            "A website asks you to 'Sign in with Google' but requests permission to 'View and delete your emails'.",
            "You find a public Wi-Fi network named 'Free_Public_WiFi_No_Password'. Should you use it for banking?",
            "A 'Photo Editor' app asks for permission to 'Always access location' even when the app is closed.",
            "You receive a request to 'Enable Accessibility Services' from a third-party App Store you just installed.",
            "A website asks for your phone number to 'verify you are human' before letting you read an article.",
            "An app asks to 'Read all notifications' to 'improve your experience'.",
            "A 'Battery Saver' app asks for 'Draw over other apps' permission.",
            "You see a 'Share your location with this website' prompt on a news site.",
            "A 'Keyboard' app from an unknown developer asks for 'Full access' to everything you type.",
            "A 'PDF Reader' asks for access to your 'Call logs' and 'Calendar'.",
            "You are asked to 'Trust this computer' when plugging your phone into a public charging station.",
            "A game asks for permission to 'Send and view SMS messages'.",
            "A website offers a 'Free Personality Quiz' but requires your Facebook profile access.",
            "An app asks to 'Disable Google Play Protect' to 'run more efficiently'.",
            "You find a 'Find my phone' app that isn't from Google or your phone manufacturer.",
            "A 'Music Player' asks for your 'Device ID and call information'.",
            "A shopping app asks for 'Bluetooth' access while you are browsing at home.",
            "You receive a notification: 'Another device is trying to sign into your account. Is this you?'",
            "An ad-blocker extension requests permission to 'Read and change all your data on all websites'.",
            "A fitness tracker app asks for access to your 'Body sensors' and 'Physical activity'.",
            "You are prompted to 'Allow background data usage' for a flashlight app.",
            "A website asks to 'Show notifications' as soon as you land on the page.",
            "A 'Privacy' app asks for 'Device Administrator' rights to 'secure your files'.",
            "An app asks you to 'Rate us 5 stars' to unlock a feature.",
            "You find a 'Modded' version of a game that requires you to uninstall the original and disable security checks.",
            "A browser extension asks to 'Manage your downloads' and 'Modify settings'.",
            "A 'QR Scanner' app asks for 'Call logs' and 'SMS' permissions.",
            "You are asked to 'Sync your contacts' to a new social media app from an unknown startup.",
            "An app asks to 'Overlay over other apps' to show 'important security alerts'.",
            "A website asks you to 'Accept all cookies' without providing a way to opt-out of tracking.",
            "You see a 'Your cloud storage is full' alert on a website you don't use.",
            "An app asks for 'Usage access' to 'optimize your battery'.",
            "A 'Voice Changer' app asks for permission to 'Record audio' and 'Modify system settings'.",
            "You are asked to 'Backup your data' to an unknown cloud provider for a 10% discount.",
            "A website asks for your 'Date of Birth' and 'Mother's Maiden Name' for a 'security verification'.",
            "An app asks to 'Search for accounts on the device' to 'simplify login'.",
            "You see a 'Click here to see who is tracking you' ad on a news site.",
            "A 'Video Downloader' app asks for 'File System' access and 'Write settings'.",
            "You receive a 'You have a new follower' email with a link that asks for your Instagram password.",
            "An app asks for 'NFC' access while you are in a crowded public place.",
            "A website asks to 'Access your camera' for a 'Virtual Try-on' of sunglasses.",
            "You find a 'Free VPN' that doesn't have a privacy policy or a physical address.",
            "An app asks to 'Ignore battery optimizations' so it can 'run better in the background'.",
            "A website asks for your 'Personal Home Address' to 'calculate shipping' for a free digital download.",
            "You see a 'Your browser is managed by your organization' message on your personal laptop.",
            "An app asks for 'Wi-Fi connection information' to 'provide localized content'.",
            "A game asks to 'Access your microphone' so you can 'talk to other players', but it's a single-player game.",
            "You receive a 'Security Key' prompt when you didn't try to log into any account."
        )
        
        val pIndex = (index - 1) % prompts.size
        return AcademyScenario(
            id = "privacy_${level}_$index",
            topicId = "privacy",
            level = level,
            question = "Scenario $index ($level): ${prompts[pIndex]}",
            options = listOf("Allow all permissions", "Allow only while using", "Deny and uninstall", "Allow once"),
            correctOptionIndex = 2,
            explanation = "Permission harvesting is how 'free' apps steal your data. Always ask: 'Does a calculator really need my location?' Denying unnecessary permissions is the first step to digital sovereignty."
        )
    }

    private fun createSafetyScenario(level: AcademyLevel, index: Int): AcademyScenario {
        val prompts = listOf(
            "Someone is posting your private home address in a public comment section.",
            "You receive a message: 'I know what you did. Send me $500 or I tell everyone.'",
            "A stranger is tagging you in offensive memes and encouraging others to mock you.",
            "Someone you don't know keeps sending you disturbing images in your DMs.",
            "A person is creating fake accounts using your name and photos to prank your friends.",
            "Someone in a gaming lobby is threatening to 'find where you live' because you won.",
            "You are being spammed with hundreds of hateful messages every hour.",
            "A contact is threatening to leak your private photos if you block them.",
            "Someone is following your every move on social media and commenting on everything you do.",
            "A group of people is coordinatedly reporting your account to get you banned for no reason.",
            "You get a text: 'I'm watching you through your window right now.'",
            "Someone is sharing edited videos of you to make you look like you said something offensive.",
            "A 'friend' is pressuring you to share your password to 'check something'.",
            "You are added to a group chat where everyone is attacking one specific person.",
            "Someone is making fun of your appearance or identity in every post you make.",
            "You receive a call from an unknown number where the person just breathes heavily.",
            "A stranger asks you for a selfie to 'prove you're real' on a gaming platform.",
            "Someone is telling you that 'nobody likes you' and you should 'just disappear'.",
            "You see a post encouraging people to self-harm as a 'challenge'.",
            "A contact keeps calling you at 3 AM despite being told to stop.",
            "Someone is using AI to make your voice say things you never said in a recording.",
            "You are being blackmailed with a screenshot of a private conversation taken out of context.",
            "A stranger is offering you 'expensive gifts' if you meet them in person without telling your parents.",
            "Someone is hacking into your friends' accounts to send you malicious links.",
            "You are being accused of something you didn't do by a large group of people online.",
            "A person is sending you your own IP address and ISP details to intimidate you.",
            "Someone is constantly 'ratio-ing' and dogpiling your every post to silence you.",
            "You receive a physical letter in the mail from someone you only know online.",
            "A stranger is asking you for 'financial help' and sends you pictures of their 'starving children'.",
            "Someone is using your photos to sell 'adult content' on a third-party site.",
            "You are being pressured to join a 'secret group' that requires an initiation fee.",
            "Someone is threatening to 'swat' you (send police to your house) during a live stream.",
            "A contact is sending you your own deleted posts from years ago to shame you.",
            "Someone is pretending to be a celebrity and asks you for 'fan support' in Bitcoin.",
            "You are being invited to a 'private party' by someone you met 5 minutes ago on a chat app.",
            "Someone is making a 'hate page' dedicated entirely to mocking you.",
            "A stranger is asking for your 'Discord Token' to 'verify your account'.",
            "Someone is threatening to 'dox' your family members because of a disagreement.",
            "You are receiving 'death threats' over a video game skin you are wearing.",
            "A person is sending you links to 'leaked' information about yourself.",
            "Someone is using your 'public' photos to train a deepfake AI model.",
            "You are being pressured to 'cancel' a friend for a minor mistake.",
            "A stranger is asking you to 'log into their account' to help them with a problem.",
            "Someone is sending you 'spoofed' messages that look like they are from your parents.",
            "You are being added to 'scam' groups on WhatsApp every day.",
            "A person is threatening to report your 'illegal' activity (which is fake) unless you pay them.",
            "Someone is asking for your 'OTP' because they 'accidentally' used your number for their login.",
            "You are being followed by 'bots' that spam your notifications with links.",
            "A stranger is asking for a 'video call' and starts performing inappropriate acts.",
            "Someone is telling you that they have 'remote access' to your webcam and shows you a fake image."
        )
        
        val sIndex = (index - 1) % prompts.size
        return AcademyScenario(
            id = "cyberbullying_${level}_$index",
            topicId = "cyberbullying",
            level = level,
            question = "Scenario $index ($level): ${prompts[sIndex]}",
            options = listOf("Argue back", "Stay silent and suffer", "Block and report", "Delete your account"),
            correctOptionIndex = 2,
            explanation = "Harassment thrives on reaction. Blocking prevents further contact, and reporting provides evidence to the platform. Never engage with bullies or extortionists."
        )
    }

    private fun createMalwareScenario(level: AcademyLevel, index: Int): AcademyScenario {
        val prompts = if (level == AcademyLevel.ADVANCED) {
            listOf(
                "You find a library on MavenCentral that has been 'hijacked' (Typosquatting) - it's called 'androidx.core-ktx-utils' instead of 'androidx.core-ktx'. It has 100k downloads. Should you use it?",
                "An app you developed is flagged by Play Protect for having an 'unsafe implementation of WebView'. It points to a JavaScript interface that has access to your app's internal database.",
                "You receive a Pull Request on your open-source project from an unknown user that adds a dependency to a 'minified' JS file hosted on a random CDN.",
                "While using ADB, you notice a process called 'kworker' using 90% CPU even when the screen is off. You didn't install any crypto miners.",
                "A system update image you downloaded manually has a SHA-256 hash that differs by one character from the hash posted on the official developer site."
            )
        } else {
            listOf(
                "You find a 'Free Premium Spotify' APK on a Telegram group. Should you install it?",
                "A pop-up on a site says: 'Your phone is slow! Click to boost RAM speed instantly.'",
                "You receive a .exe file named 'Urgent_Invoice' on your Android phone.",
                "An app asks you to 'Enable Unknown Sources' in settings before it can be installed.",
                "You find a 'Cheat Code' app for a popular game that isn't on the Play Store.",
                "A friend sends a link: 'Look at this funny cat! app-cat-funny.apk'.",
                "You see an ad for 'Free Diamonds' in your favorite game if you download a 'helper' app.",
                "A website asks you to install a 'Security Plugin' to view its content.",
                "Your phone starts showing ads on the lock screen and home screen out of nowhere.",
                "You notice your battery is draining 5x faster and the phone is always hot.",
                "A 'Flashlight' app you just installed is requesting 'Device Administrator' rights.",
                "You receive a system update notification from a browser, not from settings.",
                "An app you downloaded from a forum asks to 'Scan your files' to find 'hidden junk'.",
                "You find a 'WhatsApp Gold' version that promises exclusive features.",
                "A 'File Manager' asks for permission to 'Install other apps'.",
                "Your browser home page has changed to a weird search engine you didn't choose.",
                "You get an SMS with a link to 'Track your stolen crypto' via an app download.",
                "An app asks you to 'Input your pattern' to 'secure its settings'.",
                "You see a 'Google Play Protect is disabled' warning after installing a modded game.",
                "A 'Video Player' app asks for permission to 'Access your microphone and camera'.",
                "You receive a PDF file that triggers a 'Download required' prompt for a custom viewer.",
                "An app asks for 'Accessibility Services' to 'improve battery performance'.",
                "You find a 'Minecraft' installer on a site that is only 2MB in size.",
                "A website asks to 'Add to Home Screen' and then starts downloading a file.",
                "You are prompted to 'Update your browser' by a site that looks like a news outlet.",
                "An app you just installed doesn't have an icon and can't be found in the app drawer.",
                "You see 'Transaction Successful' notifications for apps you didn't buy.",
                "A 'Photo Recovery' app asks for your 'Root' access to find deleted files.",
                "You find a 'Free Wi-Fi Password' app that requires you to disable your firewall.",
                "A game asks to 'Send and view SMS' to 'verify your high score'.",
                "You receive an 'MMS' message with an attachment named 'VoiceMessage.apk'.",
                "A site asks you to 'Allow downloads from this source' to see a meme.",
                "An app asks to 'Modify system settings' to 'change the wallpaper'.",
                "You see 'Running in the background' notifications for an app called 'System Service'.",
                "A website asks you to install a 'Certificate' to access its secure area.",
                "An app asks to 'Read Call Log' to 'find your friends' in a racing game.",
                "You find a 'Fortnite' Android APK on a site that isn't Epic Games.",
                "A 'Cleaner' app asks to 'Clear all app data' to free up space.",
                "You see a 'Your SIM card is being updated' message while browsing a shady site.",
                "An app asks for 'Usage Stats' access to 'give you rewards'.",
                "You receive a WhatsApp message with a link to 'Upgrade to WhatsApp Premium'.",
                "A 'Wallpaper' app asks for 'Location' and 'Phone' permissions.",
                "You find a 'Hacking Tool' APK that promises to show you someone's private chats.",
                "An app asks for 'Bluetooth' permission to 'connect to its cloud server'.",
                "You see a 'Google Play Protect found a threat' notification for an app you use daily.",
                "A website asks to 'Install Web App' which is actually an APK file download.",
                "An app asks to 'Display over other apps' to 'protect your screen from blue light'.",
                "You receive an email with a link to 'Download your tax documents' which is an .apk.",
                "A 'Music' app asks to 'Write to SD card' and 'Read contacts'.",
                "You find a 'Paid app for free' on a site that has 50 different 'Download' buttons."
            )
        }
        
        val mIndex = (index - 1) % prompts.size
        return AcademyScenario(
            id = "malware_${level}_$index",
            topicId = "malware",
            level = level,
            question = "Scenario $index ($level): ${prompts[mIndex]}",
            options = if (level == AcademyLevel.ADVANCED) {
                listOf("Trust and ignore", "Verify hash/signatures", "Isolate and analyze", "Report/Uninstall")
            } else {
                listOf("Install it", "Research first", "Scan with AEGIS", "Delete immediately")
            },
            correctOptionIndex = if (level == AcademyLevel.ADVANCED) 1 else 3,
            explanation = if (level == AcademyLevel.ADVANCED) {
                "Advanced malware uses typosquatting, supply-chain attacks (Pull Requests), and system-level masquerading. Experts must verify cryptographic signatures and monitor process behavior."
            } else {
                "Malware (Viruses, Spyware, Ransomware) most often enters devices via 'Modded' or 'Free Premium' APKs from unofficial sites. Always stick to the official Play Store and keep Play Protect enabled."
            }
        )
    }
}
