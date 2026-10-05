#version 150

uniform sampler2D DiffuseSampler;
uniform float Throttle;
uniform float RippleTime;
uniform float Time;
uniform float TakeoffShake;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 baseUv = texCoord;
    vec2 center = vec2(0.5, 0.5);

    float safeThrottle = clamp(Throttle, 0.0, 1.0);

    // ==========================================
    // 1. EKRAN SARSINTISI (SHAKE)
    // ==========================================
    float throttleShakeIntensity = 0.0;

    // Vanilla/Sonic Hız Sarsıntısı (Mevcut kodun)
    if (safeThrottle > 0.0 && safeThrottle < 0.2) {
        throttleShakeIntensity = (0.2 - safeThrottle) * 5.0 * 0.005;
    } else if (safeThrottle > 0.6) {
        throttleShakeIntensity = (safeThrottle - 0.6) * 0.0035;
    }

    // YENİ: Kalkış Anı Sarsıntısı (Daha yüksek frekanslı ve şiddetli)
    float takeoffShakeOffset = 0.0;
    if (TakeoffShake > 0.0) {
        // Time * 150.0 ile çok hızlı bir buzz/zangırdama efekti yaratıyoruz
        // 0.02 çarpanı şiddeti belirler.
        takeoffShakeOffset = sin(Time * 150.0) * 0.02 * TakeoffShake;
    }

    // İki sarsıntıyı birleştir ve UV'ye uygula
    if (throttleShakeIntensity > 0.0 || TakeoffShake > 0.0) {
        // Standart hız sarsıntısı (Daha yavaş sinüzoidal)
        baseUv.x += sin(Time * 60.0) * throttleShakeIntensity;
        baseUv.y += cos(Time * 75.0) * throttleShakeIntensity;

        // Yüksek frekanslı kalkış buzz'ı
        baseUv.x += takeoffShakeOffset;
        baseUv.y -= takeoffShakeOffset; // Diyagonal sarsıntı
    }

    // ==========================================
    // 3. CHROMATIC ABERRATION & FİNAL RENK
    // ==========================================
    // ... (Chromatic kodları aynı kalıyor, değiştirmedim) ...
    float sonicFactor = max(0.0, (safeThrottle - 0.6) / 0.4);
    vec4 finalColor;

    if (sonicFactor > 0.0 && distance(baseUv, center) > 0.001) {
        float caFade = max(0.0, 1.0 - (RippleTime * 1.0));
        float caOffset = 0.008 * sonicFactor * caFade * distance(baseUv, center);
        vec2 dir = normalize(baseUv - center);

        float r = texture(DiffuseSampler, baseUv + dir * caOffset).r;
        float g = texture(DiffuseSampler, baseUv).g;
        float b = texture(DiffuseSampler, baseUv - dir * caOffset).b;

        finalColor = vec4(r, g, b, 1.0);
    } else {
        finalColor = texture(DiffuseSampler, baseUv);
    }

    finalColor.a = 1.0;
    fragColor = finalColor;
}