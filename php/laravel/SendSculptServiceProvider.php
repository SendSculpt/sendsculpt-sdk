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
            $environment = config('services.sendsculpt.environment', 'live');
            
            return new SendSculptClient($apiKey, $environment);
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
