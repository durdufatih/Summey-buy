// Seviye seviye pratik paragrafları.
// Her paragraf cümlelere bölünmüştür; her cümlenin Türkçesi ve İngilizcesi hazırdır,
// böylece pratik bölümü internet bağlantısı olmadan da çalışır.
const PARAGRAPH_LEVELS = [
  {
    id: "a1",
    name: "Başlangıç (A1)",
    paragraphs: [
      {
        id: "a1-1",
        title: "Kendimi Tanıtıyorum",
        sentences: [
          { tr: "Merhaba, benim adım Ali.", en: "Hello, my name is Ali." },
          { tr: "Ben yirmi beş yaşındayım.", en: "I am twenty-five years old." },
          { tr: "İstanbul'da yaşıyorum.", en: "I live in Istanbul." },
          { tr: "Bir bankada çalışıyorum.", en: "I work at a bank." },
          { tr: "Boş zamanlarımda kitap okumayı severim.", en: "I like reading books in my free time." }
        ]
      },
      {
        id: "a1-2",
        title: "Bir Günüm",
        sentences: [
          { tr: "Her sabah saat yedide uyanırım.", en: "I wake up at seven o'clock every morning." },
          { tr: "Kahvaltıda peynir ve zeytin yerim.", en: "I eat cheese and olives for breakfast." },
          { tr: "Otobüsle işe giderim.", en: "I go to work by bus." },
          { tr: "Akşam ailemle yemek yerim.", en: "I have dinner with my family in the evening." },
          { tr: "Gece on birde uyurum.", en: "I sleep at eleven at night." }
        ]
      },
      {
        id: "a1-3",
        title: "Ailem",
        sentences: [
          { tr: "Ailem dört kişidir.", en: "My family has four people." },
          { tr: "Babam öğretmendir.", en: "My father is a teacher." },
          { tr: "Annem doktordur.", en: "My mother is a doctor." },
          { tr: "Bir kız kardeşim var.", en: "I have a sister." },
          { tr: "Hafta sonları birlikte parka gideriz.", en: "We go to the park together on weekends." }
        ]
      }
    ]
  },
  {
    id: "a2",
    name: "Temel (A2)",
    paragraphs: [
      {
        id: "a2-1",
        title: "Tatil Planı",
        sentences: [
          { tr: "Gelecek yaz ailemle Antalya'ya gitmeyi planlıyoruz.", en: "Next summer, we are planning to go to Antalya with my family." },
          { tr: "Orada deniz kenarında bir otelde kalacağız.", en: "We will stay at a hotel by the sea there." },
          { tr: "Her gün yüzmek ve güneşlenmek istiyorum.", en: "I want to swim and sunbathe every day." },
          { tr: "Ayrıca tarihi yerleri de ziyaret edeceğiz.", en: "We will also visit historical places." },
          { tr: "Umarım hava güzel olur.", en: "I hope the weather will be nice." }
        ]
      },
      {
        id: "a2-2",
        title: "Alışveriş",
        sentences: [
          { tr: "Dün arkadaşımla alışveriş merkezine gittim.", en: "Yesterday I went to the shopping mall with my friend." },
          { tr: "Kendime yeni bir mont almak istiyordum.", en: "I wanted to buy a new coat for myself." },
          { tr: "Birçok mağazaya baktık ama hiçbirini beğenmedim.", en: "We looked at many stores, but I didn't like any of them." },
          { tr: "Sonunda indirimde olan mavi bir mont buldum.", en: "Finally, I found a blue coat that was on sale." },
          { tr: "Çok memnun kaldım çünkü fiyatı da uygundu.", en: "I was very pleased because the price was also reasonable." }
        ]
      },
      {
        id: "a2-3",
        title: "Yeni Bir Hobi",
        sentences: [
          { tr: "Geçen ay gitar çalmaya başladım.", en: "Last month I started playing the guitar." },
          { tr: "Haftada iki gün kursa gidiyorum.", en: "I go to a course two days a week." },
          { tr: "İlk başta parmaklarım çok acıyordu.", en: "At first, my fingers hurt a lot." },
          { tr: "Şimdi basit şarkılar çalabiliyorum.", en: "Now I can play simple songs." },
          { tr: "Hedefim bir yıl içinde sahneye çıkmak.", en: "My goal is to perform on stage within a year." }
        ]
      }
    ]
  },
  {
    id: "b1",
    name: "Orta (B1)",
    paragraphs: [
      {
        id: "b1-1",
        title: "Teknoloji ve Hayatımız",
        sentences: [
          { tr: "Teknoloji hayatımızın her alanını hızla değiştiriyor.", en: "Technology is rapidly changing every area of our lives." },
          { tr: "Akıllı telefonlar sayesinde bilgiye anında ulaşabiliyoruz.", en: "Thanks to smartphones, we can access information instantly." },
          { tr: "Ancak sosyal medyada çok fazla zaman geçirmek bazı sorunlara yol açabiliyor.", en: "However, spending too much time on social media can lead to some problems." },
          { tr: "Uzmanlar, ekran süresini sınırlamanın önemli olduğunu söylüyor.", en: "Experts say that limiting screen time is important." },
          { tr: "Bence teknolojiyi bilinçli kullanmak hepimizin sorumluluğu.", en: "In my opinion, using technology consciously is everyone's responsibility." }
        ]
      },
      {
        id: "b1-2",
        title: "Şehir Hayatı mı, Köy Hayatı mı?",
        sentences: [
          { tr: "Birçok insan büyük şehirlerde yaşamayı tercih ediyor.", en: "Many people prefer to live in big cities." },
          { tr: "Şehirler iş imkânları ve sosyal aktiviteler açısından zengindir.", en: "Cities are rich in job opportunities and social activities." },
          { tr: "Öte yandan, köy hayatı daha sakin ve doğayla iç içedir.", en: "On the other hand, village life is calmer and closer to nature." },
          { tr: "Trafik ve gürültü, şehir hayatının en büyük dezavantajlarından biridir.", en: "Traffic and noise are among the biggest disadvantages of city life." },
          { tr: "Emekli olduktan sonra küçük bir kasabaya taşınmayı hayal ediyorum.", en: "I dream of moving to a small town after I retire." }
        ]
      },
      {
        id: "b1-3",
        title: "Sağlıklı Yaşam",
        sentences: [
          { tr: "Sağlıklı bir yaşam için düzenli egzersiz yapmak şarttır.", en: "Regular exercise is essential for a healthy life." },
          { tr: "Dengeli beslenmek de en az spor kadar önemlidir.", en: "Eating a balanced diet is at least as important as exercising." },
          { tr: "Yeterince uyumayan insanlar gün içinde odaklanma sorunu yaşar.", en: "People who don't sleep enough have trouble focusing during the day." },
          { tr: "Stresle başa çıkmak için meditasyon yapmayı deneyebilirsiniz.", en: "You can try meditating to cope with stress." },
          { tr: "Küçük alışkanlık değişiklikleri uzun vadede büyük fark yaratır.", en: "Small habit changes make a big difference in the long run." }
        ]
      }
    ]
  },
  {
    id: "b2",
    name: "İleri (B2+)",
    paragraphs: [
      {
        id: "b2-1",
        title: "İklim Değişikliği",
        sentences: [
          { tr: "İklim değişikliği, çağımızın en ciddi küresel sorunlarından biri olarak kabul edilmektedir.", en: "Climate change is considered one of the most serious global problems of our age." },
          { tr: "Bilim insanları, sera gazı emisyonlarının acilen azaltılması gerektiği konusunda hemfikir.", en: "Scientists agree that greenhouse gas emissions must be reduced urgently." },
          { tr: "Aksi takdirde, kuraklık ve sel gibi aşırı hava olayları daha sık yaşanacak.", en: "Otherwise, extreme weather events such as droughts and floods will occur more frequently." },
          { tr: "Yenilenebilir enerji kaynaklarına yatırım yapmak bu mücadelenin kilit noktasıdır.", en: "Investing in renewable energy sources is the key point of this struggle." },
          { tr: "Bireysel çabalar küçük görünse de toplumsal dönüşümün temelini oluşturur.", en: "Although individual efforts may seem small, they form the basis of social transformation." }
        ]
      },
      {
        id: "b2-2",
        title: "Yapay Zekâ ve Gelecek",
        sentences: [
          { tr: "Yapay zekâ, son yıllarda hayatımıza beklenenden çok daha hızlı girdi.", en: "Artificial intelligence has entered our lives much faster than expected in recent years." },
          { tr: "Birçok sektörde verimliliği artırırken bazı mesleklerin geleceğini de tartışmaya açtı.", en: "While increasing efficiency in many sectors, it has also opened the future of some professions to debate." },
          { tr: "Kimi uzmanlar, yapay zekânın insan yaratıcılığını tamamlayacağını savunuyor.", en: "Some experts argue that artificial intelligence will complement human creativity." },
          { tr: "Diğerleri ise etik kuralların ve yasal düzenlemelerin yetersiz kaldığına dikkat çekiyor.", en: "Others point out that ethical rules and legal regulations remain insufficient." },
          { tr: "Kesin olan şu ki, bu teknolojiyle birlikte yaşamayı öğrenmek zorundayız.", en: "What is certain is that we have to learn to live with this technology." }
        ]
      },
      {
        id: "b2-3",
        title: "Okumanın Önemi",
        sentences: [
          { tr: "Düzenli kitap okumak, kelime dağarcığını zenginleştirmenin en etkili yollarından biridir.", en: "Reading books regularly is one of the most effective ways to enrich your vocabulary." },
          { tr: "Araştırmalar, okuyan bireylerin empati kurma becerilerinin daha gelişmiş olduğunu gösteriyor.", en: "Research shows that individuals who read have more developed empathy skills." },
          { tr: "Ne yazık ki dijital çağda uzun metinlere odaklanmak giderek zorlaşıyor.", en: "Unfortunately, in the digital age, focusing on long texts is becoming increasingly difficult." },
          { tr: "Günde yalnızca yirmi dakika okumak bile zamanla büyük bir birikim sağlar.", en: "Even reading for only twenty minutes a day provides great accumulation over time." },
          { tr: "Unutmayalım ki okumak, zihnimiz için en iyi egzersizlerden biridir.", en: "Let's not forget that reading is one of the best exercises for our minds." }
        ]
      }
    ]
  }
];
