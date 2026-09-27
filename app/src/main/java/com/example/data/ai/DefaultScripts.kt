package com.example.data.ai

import com.example.data.model.ScriptLanguage
import com.example.data.model.SceneItem
import com.example.data.model.SubtitleSegment
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoDurationOption
import com.example.data.model.VideoSettings
import com.example.data.model.VideoStyle
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceSpeed

object DefaultScripts {

    fun getSampleScript(language: ScriptLanguage): String {
        return when (language) {
            ScriptLanguage.BENGALI -> """
একটি শান্ত গ্রামের প্রান্তে এক চতুর শিয়াল আর তার তৈরি ছোট্ট এক রোবট বন্ধু থাকত। 
একদিন তাদের তৈরি রোবটটি ভুল করে মিষ্টির দোকানের সব মিষ্টি খেয়ে ফেলে! 
দোকানদার কাকা যখন রেগে ছুটে এল, শিয়াল আর রোবট মিলে মিষ্টি বানানোর একটি দারুণ গান গাইতে শুরু করল। 
গান শুনে দোকানদার কাকা হেসে ফেলল এবং বলল, "তোমরা আমার দোকানের সেরা শেফ!" 
তাদের বন্ধুত্বের গল্প সারা গ্রামে ছড়িয়ে পড়ল।
            """.trimIndent()

            ScriptLanguage.HINDI -> """
एक शांत पहाड़ी गाँव के किनारे एक नटखट लोमड़ी और उसका बनाया छोटा रोबोट दोस्त रहते थे। 
एक दिन उस प्यारे रोबोट ने गलती से हलवाई की दुकान की सारी गरम जलेबियाँ खा लीं! 
जब हलवाई काका गुस्से में डंडा लेकर दौड़े, तो लोमड़ी और रोबोट ने मिलकर एक मजेदार जलेबी डांस और गाना शुरू कर दिया। 
उनकी मासूमियत और अनोखा हुनर देखकर काका जोर से हँस पड़े और बोले, "तुम दोनों ही आज से मेरी दुकान के मास्टरशेफ हो!" 
उनकी अनोखी दोस्ती की चर्चा पूरे गाँव में छा गई।
            """.trimIndent()

            ScriptLanguage.ENGLISH -> """
At the sunny edge of Whisper Valley lived Felix the quick-witted fox and his clockwork robotic pet, Bolt. 
One breezy afternoon, hungry little Bolt accidentally swallowed every single blueberry pie from Mrs. Gable’s bakery! 
When furious Mrs. Gable stormed outside waving a wooden spatula, Felix tossed Bolt a chef’s apron and struck a dramatic baker's pose. 
Bolt spun on one wheel, whisking flour into whimsical clouds while singing a catchy baking tune. 
Mrs. Gable burst into laughter, declaring them official bakery partners. From that day on, sweet treats and laughter filled the valley.
            """.trimIndent()
        }
    }

    fun getDemoScenes(language: ScriptLanguage): List<SceneItem> {
        val style = "cartoon style, warm lighting, animated 3D aesthetic"
        return when (language) {
            ScriptLanguage.BENGALI -> listOf(
                SceneItem(
                    sceneNumber = 1,
                    sceneDescription = "গ্রামের প্রান্তে শিয়াল ও তার উজ্জ্বল ধাতব রোবট বন্ধু একটি ছোট কাঠের ওয়ার্কশপে খেলছে।",
                    characterDescription = "একটি উজ্জ্বল বাদামী চতুর শিয়াল এবং নীল চোখের পিতলের ছোট কিউট রোবট।",
                    backgroundDescription = "রৌদ্রোজ্জ্বল সবুজ গ্রাম্য উপত্যকা এবং রঙিন কাঠের কুটির।",
                    cameraDirection = "ওয়াইড এস্টাবলিশিং শট, প্যানিং রাইট।",
                    dialogue = "শান্ত গ্রামের প্রান্তে এক চতুর শিয়াল আর তার তৈরি ছোট্ট এক রোবট বন্ধু থাকত।",
                    estimatedDurationSec = 4,
                    visualPrompt = "A clever vibrant orange fox wearing a teal vest sitting next to a cute mini brass robot with glowing cyan eyes, inside a cozy rustic wooden workshop, $style",
                    subtitles = listOf(
                        SubtitleSegment("শান্ত গ্রামের প্রান্তে এক চতুর শিয়াল", 0, 2000),
                        SubtitleSegment("আর তার তৈরি ছোট্ট এক রোবট বন্ধু থাকত।", 2000, 4000)
                    )
                ),
                SceneItem(
                    sceneNumber = 2,
                    sceneDescription = "রোবটটি মিষ্টির দোকানে দাঁড়িয়ে আনন্দের সাথে লাল গোলাপজামুন ও রসগোল্লা খাচ্ছে।",
                    characterDescription = "নীল চোখের পিতলের কিউট রোবট, মুখে মিষ্টির সিরা লেগে আছে।",
                    backgroundDescription = "গ্রামের ঐতিহ্যবাহী মিষ্টির দোকান, মাটির হাঁড়ি ও কাঁচের শোকেস।",
                    cameraDirection = "ক্লোজ-আপ শট, রোবটের হাসিমুখ।",
                    dialogue = "একদিন তাদের তৈরি রোবটটি ভুল করে মিষ্টির দোকানের সব মিষ্টি খেয়ে ফেলে!",
                    estimatedDurationSec = 4,
                    visualPrompt = "Cute mini brass robot comically covered in sweet syrup surrounded by empty clay dessert bowls in a traditional sweet shop, $style",
                    subtitles = listOf(
                        SubtitleSegment("একদিন তাদের তৈরি রোবটটি ভুল করে", 0, 1800),
                        SubtitleSegment("মিষ্টির দোকানের সব মিষ্টি খেয়ে ফেলে!", 1800, 4000)
                    )
                ),
                SceneItem(
                    sceneNumber = 3,
                    sceneDescription = "শিয়াল ও রোবট একসঙ্গে নেচে নেচে গান গাইছে, দোকানদার কাকা হেসে ফেলছেন।",
                    characterDescription = "বাদামী শিয়াল ও কিউট রোবট শেফের টুপি পরে হাস্যকর ভঙ্গি করছে, দয়ালু বুড়ো দোকানদার কাকা হাসছেন।",
                    backgroundDescription = "মিষ্টির দোকানের সামনে রঙিন ফুলের বাগান।",
                    cameraDirection = "মিডিয়াম শট, আনন্দঘন ও প্রাণবন্ত।",
                    dialogue = "গান শুনে দোকানদার কাকা হেসে ফেলল এবং বলল, 'তোমরা আমার সেরা শেফ!'",
                    estimatedDurationSec = 5,
                    visualPrompt = "Clever orange fox and cute mini robot both wearing oversized white chef hats singing joyfully, while kind elderly shopkeeper laughs heartily, $style",
                    subtitles = listOf(
                        SubtitleSegment("গান শুনে দোকানদার কাকা হেসে ফেলল", 0, 2500),
                        SubtitleSegment("এবং বলল, 'তোমরা আমার সেরা শেফ!'", 2500, 5000)
                    )
                )
            )

            ScriptLanguage.HINDI -> listOf(
                SceneItem(
                    sceneNumber = 1,
                    sceneDescription = "पहाड़ी गाँव के सुंदर किनारे चालाक लोमड़ी और उसका रोबोट साथी साथ बैठे हैं।",
                    characterDescription = "सुनहरी चंचल लोमड़ी और चमकदार नीली आँखों वाला छोटा दोस्ताना रोबोट।",
                    backgroundDescription = "हरी-भरी पहाड़ियाँ, रंग-बिरंगी झोपड़ियाँ और नीला आसमान।",
                    cameraDirection = "सिनेमैटिक वाइड शॉट, धीरे-धीरे ज़ूम इन।",
                    dialogue = "एक शांत पहाड़ी गाँव के किनारे एक नटखट लोमड़ी और उसका रोबोट दोस्त रहते थे।",
                    estimatedDurationSec = 4,
                    visualPrompt = "A playful golden-red fox wearing a little scarf sitting beside a tiny friendly robot with glowing blue eyes on a grassy hill overlooking a scenic Indian village, $style",
                    subtitles = listOf(
                        SubtitleSegment("एक शांत पहाड़ी गाँव के किनारे", 0, 2000),
                        SubtitleSegment("एक नटखट लोमड़ी और उसका रोबोट दोस्त रहते थे।", 2000, 4000)
                    )
                ),
                SceneItem(
                    sceneNumber = 2,
                    sceneDescription = "हलवाई की दुकान में रोबोट गरमागरम जलेबियाँ मजे से खा रहा है।",
                    characterDescription = "छोटा रोबोट, हाथ में जलेबी और चेहरे पर शरारती मुस्कान।",
                    backgroundDescription = "भारतीय पारंपरिक हलवाई की दुकान, कड़ाही और मिठाइयों के थाल।",
                    cameraDirection = "फोकस्ड मीडियम शॉट, मज़ाकिया माहौल।",
                    dialogue = "एक दिन उस प्यारे रोबोट ने गलती से हलवाई की दुकान की सारी गरम जलेबियाँ खा लीं!",
                    estimatedDurationSec = 4,
                    visualPrompt = "Mini metallic robot gleefully eating crispy golden jalebis from an ornate brass tray in a lively village sweet stall, $style",
                    subtitles = listOf(
                        SubtitleSegment("एक दिन उस प्यारे रोबोट ने गलती से", 0, 2000),
                        SubtitleSegment("हलवाई की दुकान की सारी जलेबियाँ खा लीं!", 2000, 4000)
                    )
                ),
                SceneItem(
                    sceneNumber = 3,
                    sceneDescription = "लोमड़ी और रोबोट बावर्ची की टोपी पहनकर नाचते हैं, हलवाई काका मुस्कुराते हैं।",
                    characterDescription = "लोमड़ी और रोबोट शेफ टोपी में, हलवाई काका खुश होकर ताली बजाते हैं।",
                    backgroundDescription = "रंग-बिरंगी रोशनी और फूलों से सजी मिठाई की दुकान।",
                    cameraDirection = "वाइड शॉट, दिल छू लेने वाला दृश्य।",
                    dialogue = "काका हँस पड़े और बोले, 'तुम दोनों ही आज से मेरी दुकान के मास्टरशेफ हो!'",
                    estimatedDurationSec = 5,
                    visualPrompt = "Golden fox and cute robot doing a silly joyful chef dance while kind shopkeeper uncle claps with happiness in warm sunlight, $style",
                    subtitles = listOf(
                        SubtitleSegment("काका हँस पड़े और बोले,", 0, 2000),
                        SubtitleSegment("'तुम दोनों ही आज से मेरी दुकान के मास्टरशेफ हो!'", 2000, 5000)
                    )
                )
            )

            ScriptLanguage.ENGLISH -> listOf(
                SceneItem(
                    sceneNumber = 1,
                    sceneDescription = "Felix the fox and his metallic clockwork pet Bolt tinkering in a sun-drenched hill workshop.",
                    characterDescription = "Felix, an auburn fox with emerald green eyes and a mechanic's tool belt; Bolt, a spherical brass robot with glowing cyan optical sensors.",
                    backgroundDescription = "Whimsical fantasy valley filled with blooming lavender and gentle windmills.",
                    cameraDirection = "Cinematic wide angle, slow push in towards the workshop window.",
                    dialogue = "At the sunny edge of Whisper Valley lived Felix the quick-witted fox and his clockwork robotic pet, Bolt.",
                    estimatedDurationSec = 5,
                    visualPrompt = "Felix a sharp witty auburn fox with emerald eyes wearing a leather work apron beside Bolt a tiny round brass robot with cyan eyes in an enchanting valley, $style",
                    subtitles = listOf(
                        SubtitleSegment("At the sunny edge of Whisper Valley", 0, 2400),
                        SubtitleSegment("lived Felix the fox and his robotic pet, Bolt.", 2400, 5000)
                    )
                ),
                SceneItem(
                    sceneNumber = 2,
                    sceneDescription = "Bolt standing atop a rustic wooden counter having devoured a tray of fresh blueberry pies.",
                    characterDescription = "Bolt the brass robot looking guilty with blue pie filling smeared across his faceplate; Felix holding his head in comical disbelief.",
                    backgroundDescription = "Cozy village bakery interior with flour dusted tables and hanging copper pots.",
                    cameraDirection = "Low angle medium shot focusing on the guilty robot.",
                    dialogue = "One breezy afternoon, hungry little Bolt accidentally swallowed every single blueberry pie from Mrs. Gable’s bakery!",
                    estimatedDurationSec = 5,
                    visualPrompt = "Tiny round brass robot Bolt with blue pie filling on face sitting atop a rustic bakery table as an orange fox gasps in shock, $style",
                    subtitles = listOf(
                        SubtitleSegment("One afternoon, hungry little Bolt accidentally swallowed", 0, 2600),
                        SubtitleSegment("every single blueberry pie from Mrs. Gable's bakery!", 2600, 5000)
                    )
                ),
                SceneItem(
                    sceneNumber = 3,
                    sceneDescription = "Mrs. Gable bursts into laughter as Felix and Bolt perform an impromptu acrobatic baking routine.",
                    characterDescription = "Felix balancing rolling pins, Bolt spinning while whisking cream, Mrs. Gable smiling warmly with hands on hips.",
                    backgroundDescription = "Sunny bakery storefront surrounded by cheerful village onlookers.",
                    cameraDirection = "Dynamic tracking shot capturing the flour swirl and joyful expressions.",
                    dialogue = "Mrs. Gable burst into laughter, declaring them official bakery partners. Sweet treats and laughter filled the valley.",
                    estimatedDurationSec = 5,
                    visualPrompt = "Joyous animated scene of witty orange fox and round brass robot juggling rolling pins and baking with a cheerful baker woman laughing, $style",
                    subtitles = listOf(
                        SubtitleSegment("Mrs. Gable burst into laughter, declaring them bakery partners.", 0, 2800),
                        SubtitleSegment("Sweet treats and laughter filled the valley.", 2800, 5000)
                    )
                )
            )
        }
    }
}
