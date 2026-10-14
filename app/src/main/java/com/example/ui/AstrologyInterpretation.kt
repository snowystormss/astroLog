package com.example.ui

object AstrologyInterpretation {

    fun getSummary(sunSign: String, moonSign: String): String {
        return "Your core identity is grounded in $sunSign, combined with the emotional depth of a $moonSign Moon. This powerful combination shapes your unique path and how you connect with the world."
    }

    fun getPlanetSignText(planet: String, sign: String): String {
        return when (planet) {
            "Sun" -> when (sign) {
                "Aries" -> "As an Aries, you're first in line—literally, figuratively, and zodiacally. You're a pioneer and a natural leader who embraces new experiences."
                "Taurus" -> "You're something of an enigma in this fast-paced world, preferring the slow, steady approach. Whatever you do, you do thoroughly and completely."
                "Gemini" -> "Multitasking is one of Gemini's greatest talents. Information fuels your mind and body, and you're forever in search of the latest news."
                "Cancer" -> "The quest for security is one of your main life motivations. You have an incredible inner strength with an outward appearance of supreme confidence."
                "Leo" -> "You're the star of the zodiac, an attention-getter wherever you go. Underlying your flamboyant style is a powerful drive to rise to the top."
                "Virgo" -> "You're meticulous and have a knack for detail and analysis. You enjoy planning and take pride in performing a valuable service."
                "Libra" -> "Libra's life mission is twofold: balance and relationships. Your people skills are among the best, and your sociable nature boosts your popularity."
                "Scorpio" -> "Scorpio is mysterious and intense. You do nothing halfway, and your powerful inner strength sees you through seemingly insurmountable obstacles."
                "Sagittarius" -> "As a Sagittarius, you're an adventurer moving through life to fulfill your quest for knowledge. You see opportunities where others see boundaries."
                "Capricorn" -> "Capricorn is the sign of pure ambition. You're motivated by a status-seeking drive to rise to the top and appreciate that true success requires effort."
                "Aquarius" -> "You're an independent soul who values privacy and freedom. You embody unique traits and are naturally drawn to humanitarian ideals."
                "Pisces" -> "As a Pisces, you are caring and compassionate. You reach out to others, ever sensitive to their feelings, driven by deep imagination and intuition."
                else -> "Your Sun in $sign represents your core ego and life force."
            }
            "Moon" -> when (sign) {
                "Aries" -> "Courageous and confident, you openly express your emotions and respond impulsively with a pioneering spirit."
                "Taurus" -> "Your emotions are steady, on an even keel, finding comfort in stability and material security."
                "Gemini" -> "Change is the only constant in your emotional life. You process your feelings through thought and communication."
                "Cancer" -> "Deep emotional ties to loved ones define you. You are a natural nurturer, highly receptive to others."
                "Leo" -> "You need recognition and want to be admired. You express your feelings grandly and are incredibly loyal to loved ones."
                "Virgo" -> "Work and helping others bring you great emotional satisfaction. You process feelings analytically."
                "Libra" -> "Your emotional well-being is tied to close, affectionate relationships. Harmony and fairness are vital to you."
                "Scorpio" -> "Deeply passionate, your emotions are intense and unwavering. You are fiercely protective of your private world."
                "Sagittarius" -> "You avoid emotional ties that limit your independence. You view the world through a lens of optimistic philosophical truth."
                "Capricorn" -> "You're an ambitious hard worker who invests emotional energy in achieving goals, often appearing reserved."
                "Aquarius" -> "Passionate about humanitarian issues, your emotional reactions are often intellectualized and detached yet friendly."
                "Pisces" -> "Sensitive and caring, you are a sympathetic soul whose deep feelings strongly resonate with your environment."
                else -> "Your Moon in $sign reflects your emotional landscape and comfort zone."
            }
            "Mercury" -> when (sign) {
                "Aries" -> "Quick decisions and rapid thought processes are your style. You learn fast and speak directly."
                "Taurus" -> "Your practical mind thoroughly ponders every decision before you formulate unshakeable conclusions."
                "Gemini" -> "Information fuels your active intellect; you're versatile, curious, and quick-witted in conversation."
                "Cancer" -> "Your thinking is heavily influenced by emotion and memory, intuitively grasping the feelings of those around you."
                "Leo" -> "You express your ideas with dramatic flair and possess strong leadership and presentation skills."
                "Virgo" -> "Your mind is highly analytical and meticulous, excelling at planning, organization, and practical details."
                "Libra" -> "You are diplomatic in thought, always seeking balance and weighing all sides of an issue."
                "Scorpio" -> "Your mind acts like a detective, probing deeply to uncover the hidden truth behind everyday situations."
                "Sagittarius" -> "You think in broad strokes, guided by high principles and an optimistic, visionary perspective."
                "Capricorn" -> "Realistic, logical thinking is your specialty. You focus on practical solutions and steady progress."
                "Aquarius" -> "You have an inventive, futuristic mind that generates highly original and unconventional ideas."
                "Pisces" -> "Your thoughts are highly imaginative and intuitive, often seeing the world in pictures rather than words."
                else -> "Your Mercury in $sign guides your communication style."
            }
            "Venus" -> when (sign) {
                "Aries" -> "You pursue love and desires with impulsive passion and thrive on the excitement of romantic conquest."
                "Taurus" -> "You seek lasting, comfortable relationships and value material aesthetics and loyalty highly."
                "Gemini" -> "Variety excites you in relationships. You value a partner who provides constant mental stimulation."
                "Cancer" -> "In love, you are deeply nurturing and seek profound emotional security and a traditional home life."
                "Leo" -> "Love is a grand drama to you; you express your affections generously and expect romantic devotion."
                "Virgo" -> "You show love through acts of service and hold incredibly high standards for your romantic partners."
                "Libra" -> "Partnership is essential to your happiness; you strive for perfectly balanced and harmonious relationships."
                "Scorpio" -> "Your romantic bonds are intense, profound, and deeply transformative, demanding complete loyalty."
                "Sagittarius" -> "You view love as an adventure and require immense freedom and honesty in your relationships."
                "Capricorn" -> "You approach relationships cautiously, seeking a partner who aligns with your practical goals and ambitions."
                "Aquarius" -> "Your ideal partner is first and foremost your best friend, someone who respects your unique individuality."
                "Pisces" -> "You are a true romantic, capable of unconditional love, compassion, and deep spiritual connection."
                else -> "Your Venus in $sign influences what you attract and desire."
            }
            "Mars" -> when (sign) {
                "Aries" -> "You are pure, high-powered energy. Bold and courageous, you assert yourself passionately."
                "Taurus" -> "Your willpower and determination are unparalleled. You take slow but unstoppably steady action."
                "Gemini" -> "Your energy is dispersed across myriad mental paths. You act quickly but might struggle to maintain focus."
                "Cancer" -> "Feelings dictate your drive. You act fiercely to protect everything and everyone within your inner circle."
                "Leo" -> "You pursue your goals with majestic confidence, eager to take center stage and lead the charge."
                "Virgo" -> "You apply your energy to perfect the details, taking action through precise, efficient, and methodical steps."
                "Libra" -> "You assert yourself best in cooperative settings, channeling your drive toward establishing justice and fairness."
                "Scorpio" -> "You pursue objectives with formidable intensity and secrecy, overcoming obstacles flawlessly."
                "Sagittarius" -> "You act with enthusiastic optimism, constantly directing your energy toward exploring expansive new horizons."
                "Capricorn" -> "Your ambitions are incredibly disciplined. You act with strategic caution to climb up the mountain of success."
                "Aquarius" -> "You rebel against the conventional. You take unpredictable, independent actions to establish your originality."
                "Pisces" -> "Your drive is motivated by spiritual compassion. You act based on an inner sense of empathy and imagination."
                else -> "Your Mars in $sign reflects your drive and energy."
            }
            else -> "$planet in $sign plays a role in your astrological profile."
        }
    }

    fun getHoroscopeLove(sun: String, venus: String): String {
        return "With your Sun in $sun and Venus in $venus, today's planetary transits emphasize connecting deeply with others. Be mindful of your communication to ensure your expressions of love are received harmoniously."
    }

    fun getHoroscopeCareer(sun: String, mars: String): String {
        return "Your $sun energy coupled with your active $mars Mars points to a productive day. Focus your ambition, prioritize tasks effectively, and channel your drive to tackle challenges head-on without hesitating."
    }

    fun getHoroscopeGrowth(moon: String): String {
        return "The emotional reflection of your $moon Moon suggests today is a time for introspection. Pause to recenter yourself, acknowledge your intuitive hunches, and allow yourself space to grow spiritually."
    }

    fun generateChildhood(moon: String): String {
        return "Your early childhood base was deeply influenced by your $moon Moon. It colored your emotional habits and taught you to navigate familial expectations, creating the foundation for how you seek security today."
    }

    fun generateFuture(sun: String): String {
        return "As an $sun, the upcoming years will ask you to step boldly into your innate potential. Embrace changes that honor your natural strengths to achieve profound personal evolution."
    }

    fun generateCompatibility(sun1: String, sun2: String, venus1: String, venus2: String): String {
        return "User 1 (Sun $sun1, Venus $venus1) and User 2 (Sun $sun2, Venus $venus2) have a unique dynamic. Your differing elemental priorities mean you must be mindful of how you compromise, yet the contrast can offer powerful, unexpected areas of mutual growth and romance."
    }
}
