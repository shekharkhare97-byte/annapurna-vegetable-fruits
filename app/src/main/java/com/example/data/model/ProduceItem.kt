package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

enum class ProduceCategory(val marathiLabel: String) {
    ALL("सर्व"),
    VEGETABLE("ताज्या भाज्या"),
    FRUIT("रसाळ फळे")
}

data class ProduceItem(
    val id: String,
    val marathiName: String,
    val englishKeywords: String,
    val category: ProduceCategory,
    val priceRupees: Int,
    val unitMarathi: String,
    val badgeMarathi: String,
    val descriptionMarathi: String,
    @DrawableRes val imageRes: Int
)

object ProduceCatalog {
    val items: List<ProduceItem> = listOf(
        ProduceItem(
            id = "veg_tomato",
            marathiName = "लालेलाल गावरान टोमॅटो",
            englishKeywords = "tomato tamatar red fresh",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 40,
            unitMarathi = "१ किलो",
            badgeMarathi = "आजचा ताजा माल",
            descriptionMarathi = "रसाळ आणि आंबट-गोड गावरान टोमॅटो — भाजी, सार आणि कोशिंबिरीसाठी उत्तम.",
            imageRes = R.drawable.img_prod_tomato
        ),
        ProduceItem(
            id = "veg_okra",
            marathiName = "कोवळी हिरवी भेंडी",
            englishKeywords = "okra ladyfinger bhendi green",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 60,
            unitMarathi = "१ किलो",
            badgeMarathi = "शेतकरी थेट",
            descriptionMarathi = "मऊ आणि कोवळी हिरवी भेंडी — कुरकुरीत भेंडी मसाला किंवा परतून भाजीसाठी खास.",
            imageRes = R.drawable.img_prod_okra
        ),
        ProduceItem(
            id = "veg_brinjal",
            marathiName = "ताजी जांभळी वांगी",
            englishKeywords = "brinjal eggplant vangi baingan",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 50,
            unitMarathi = "१ किलो",
            badgeMarathi = "भरली वांगी स्पेशल",
            descriptionMarathi = "चवदार अस्सल महाराष्ट्रीयन भरली वांगी आणि वांग्याचे भरीत बनवण्यासाठी निवडक वांगी.",
            imageRes = R.drawable.img_prod_brinjal
        ),
        ProduceItem(
            id = "veg_spinach",
            marathiName = "ताजा हिरवागार पालक",
            englishKeywords = "spinach palak leafy greens",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 25,
            unitMarathi = "१ मोठी जुडी",
            badgeMarathi = "सकाळची तोडणी",
            descriptionMarathi = "लोहयुक्त कोवळा पालक — पालक पनीर, गरगटा भाजी आणि पराठ्यासाठी आरोग्यदायी.",
            imageRes = R.drawable.img_prod_spinach
        ),
        ProduceItem(
            id = "veg_potato",
            marathiName = "निवडक गावरान बटाटा",
            englishKeywords = "potato batata aloo",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 35,
            unitMarathi = "१ किलो",
            badgeMarathi = "दररोज उपयुक्त",
            descriptionMarathi = "बटाटा भाजी, वडापाव, पराठा आणि उपवासाच्या पदार्थांसाठी स्वच्छ निवडक बटाटे.",
            imageRes = R.drawable.img_prod_potato
        ),
        ProduceItem(
            id = "veg_onion",
            marathiName = "नाशिक लाल कांदा",
            englishKeywords = "onion kanda nashik red",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 45,
            unitMarathi = "१ किलो",
            badgeMarathi = "नाशिक स्पेशल",
            descriptionMarathi = "झणझणीत रस्सा, कांदा भजी आणि पोह्यांची चव वाढवणारा अस्सल नाशिक कांदा.",
            imageRes = R.drawable.img_prod_onion
        ),
        ProduceItem(
            id = "veg_cauliflower",
            marathiName = "ताजा पांढराशुभ्र फ्लॉवर",
            englishKeywords = "cauliflower flower gobi",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 35,
            unitMarathi = "१ नग (सुमारे ६०० ग्रॅम)",
            badgeMarathi = "शेतातून ताजा",
            descriptionMarathi = "पावभाजी, फ्लॉवर-मटार रस्सा आणि पुलावसाठी घट्ट व स्वच्छ पांढराशुभ्र फ्लॉवर.",
            imageRes = R.drawable.img_prod_cauliflower
        ),
        ProduceItem(
            id = "veg_carrot",
            marathiName = "गोड लाल-केशरी गाजर",
            englishKeywords = "carrot gajar orange",
            category = ProduceCategory.VEGETABLE,
            priceRupees = 50,
            unitMarathi = "१ किलो",
            badgeMarathi = "व्हिटॅमिन युक्त",
            descriptionMarathi = "कोशिंबीर, ज्यूस आणि स्वादिष्ट गाजर हलव्यासाठी ताजी कुरकुरीत गाजरे.",
            imageRes = R.drawable.img_prod_carrot
        ),
        ProduceItem(
            id = "fruit_mango",
            marathiName = "रत्नागिरी हापूस आंबा",
            englishKeywords = "mango hapus alphonso amba",
            category = ProduceCategory.FRUIT,
            priceRupees = 480,
            unitMarathi = "१ डझन (६ नग पेटी)",
            badgeMarathi = "कोकणचा राजा",
            descriptionMarathi = "सुगंधी, नैसर्गिकरीत्या पिकलेला मधुर रत्नागिरी हापूस आंबा — आमरसासाठी सर्वोत्तम.",
            imageRes = R.drawable.img_prod_mango
        ),
        ProduceItem(
            id = "fruit_banana",
            marathiName = "जळगाव गोड केळी",
            englishKeywords = "banana keli jalgaon yellow",
            category = ProduceCategory.FRUIT,
            priceRupees = 60,
            unitMarathi = "१ डझन",
            badgeMarathi = "ऊर्जादायी फळ",
            descriptionMarathi = "नैसर्गिक गोडवा असलेली जळगावची ताजी पिवळीधमक केळी — उपवास व नाश्त्यासाठी उत्तम.",
            imageRes = R.drawable.img_prod_banana
        ),
        ProduceItem(
            id = "fruit_pomegranate",
            marathiName = "सोलापुरी भगवा डाळिंब",
            englishKeywords = "pomegranate dalimb anar red",
            category = ProduceCategory.FRUIT,
            priceRupees = 140,
            unitMarathi = "१ किलो",
            badgeMarathi = "माणिक दाणे",
            descriptionMarathi = "रसरशीत लालचुटूक दाण्यांनी भरलेले गोड सोलापुरी भगवा डाळिंब.",
            imageRes = R.drawable.img_prod_pomegranate
        ),
        ProduceItem(
            id = "fruit_apple",
            marathiName = "काश्मिरी लाल सफरचंद",
            englishKeywords = "apple safarchand seb kashmiri",
            category = ProduceCategory.FRUIT,
            priceRupees = 180,
            unitMarathi = "१ किलो",
            badgeMarathi = "प्रीमियम दर्जा",
            descriptionMarathi = "कुरकुरीत, रसाळ आणि गोड काश्मिरी सफरचंद — लहानांपासून मोठ्यांपर्यंत सर्वांसाठी पौष्टिक.",
            imageRes = R.drawable.img_prod_apple
        )
    )

    private val byId = items.associateBy { it.id }

    fun findById(id: String): ProduceItem? = byId[id]
}
