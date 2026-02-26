<?php

namespace App\Providers;

use Illuminate\Support\ServiceProvider;
use SendSculpt\SendSculptClient;

class SendSculptServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     *
     * @return void
     */
    public function register()
    {
        $this->app->singleton(SendSculptClient::class, function ($app) {
            $apiKey = config('services.sendsculpt.key');
            $baseUrl = config('services.sendsculpt.url', 'https://api.sendsculpt.com/api/v1');
            
            return new SendSculptClient($apiKey, $baseUrl);
        });
    }

    /**
     * Bootstrap any application services.
     *
     * @return void
     */
    public function boot()
    {
        //
    }
}
