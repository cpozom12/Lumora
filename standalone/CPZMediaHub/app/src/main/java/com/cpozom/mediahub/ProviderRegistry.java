package com.cpozom.mediahub;

final class ProviderRegistry {
    static final Provider[] PROVIDERS = new Provider[] {
        new Provider(R.string.provider_movistar, "pe.movistar.go"),
        new Provider(R.string.provider_netflix, "com.netflix.mediaclient"),
        new Provider(R.string.provider_disney, "com.disney.disneyplus"),
        new Provider(R.string.provider_youtube, "com.google.android.youtube"),
        new Provider(R.string.provider_prime_video, "com.amazon.avod.thirdpartyclient"),
        new Provider(R.string.provider_max, "com.wbd.stream")
    };

    private ProviderRegistry() {}

    static final class Provider {
        final int labelRes;
        final String packageName;

        Provider(int labelRes, String packageName) {
            this.labelRes = labelRes;
            this.packageName = packageName;
        }
    }
}
