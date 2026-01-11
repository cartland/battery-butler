The main screen is a list of devices. Each device is a card.

Each card has an icon for the device type on the left. These are preset icons, like Motion Sensor, Button, Door Lock, etc.

The middle has two pieces of text. Top: Device Name. Bottom (smaller text): Device Type Name (matches the icon).

The right side shows a battery icon (gray, always the same), with a number underneath it to show the battery "age" (days since the battery was changed).




Here is a proposal in HTML:

<!DOCTYPE html>
<html class="dark" lang="en"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>Device List Screen</title>
<link href="https://fonts.googleapis.com" rel="preconnect"/>
<link crossorigin="" href="https://fonts.gstatic.com" rel="preconnect"/>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<script id="tailwind-config">
        tailwind.config = {
            darkMode: "class",
            theme: {
                extend: {
                    colors: {
                        "primary": "#137fec",
                        "background-light": "#f6f7f8",
                        "background-dark": "#101922",
                    },
                    fontFamily: {
                        "display": ["Inter", "sans-serif"]
                    },
                    borderRadius: {"DEFAULT": "0.25rem", "lg": "0.5rem", "xl": "0.75rem", "full": "9999px"},
                },
            },
        }
    </script>
<style>.no-scrollbar::-webkit-scrollbar {
            display: none;
        }
        .no-scrollbar {
            -ms-overflow-style: none;
            scrollbar-width: none;
        }
    </style>
<style>
    body {
      min-height: max(884px, 100dvh);
    }
  </style>
<style>
    body {
      min-height: max(884px, 100dvh);
    }
  </style>
  </head>
<body class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-white font-display antialiased h-screen flex flex-col overflow-hidden selection:bg-primary/30">
<header class="flex-none pt-12 pb-4 px-4 bg-background-light dark:bg-background-dark z-10">
<div class="flex items-center justify-between">
<h1 class="text-3xl font-bold tracking-tight text-slate-900 dark:text-white">My Devices</h1>
<button class="flex items-center justify-center w-10 h-10 rounded-full bg-primary text-white hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20">
<span class="material-symbols-outlined" style="font-size: 24px;">add</span>
</button>
</div>
</header>
<main class="flex-1 overflow-y-auto no-scrollbar px-4 pb-24 space-y-3">
<div class="group relative flex items-center justify-between p-4 bg-white dark:bg-[#1c242d] rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 transition-transform active:scale-[0.98]">
<div class="flex items-center gap-4">
<div class="flex items-center justify-center w-12 h-12 rounded-lg bg-blue-100 dark:bg-[#28323e] text-primary dark:text-white shrink-0">
<span class="material-symbols-outlined filled" style="font-variation-settings: 'FILL' 1;">lock</span>
</div>
<div class="flex flex-col">
<h3 class="text-base font-semibold text-slate-900 dark:text-white leading-tight">Front Door</h3>
<p class="text-sm text-slate-500 dark:text-slate-400">Smart Lock</p>
</div>
</div>
<div class="flex flex-col items-center justify-center min-w-[60px]">
<span class="material-symbols-outlined text-slate-400 dark:text-slate-500 mb-0.5" style="font-size: 24px; font-variation-settings: 'FILL' 1;">battery_full</span>
<span class="text-xs font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap tabular-nums">45 days</span>
</div>
</div>
<div class="group relative flex items-center justify-between p-4 bg-white dark:bg-[#1c242d] rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 transition-transform active:scale-[0.98]">
<div class="flex items-center gap-4">
<div class="flex items-center justify-center w-12 h-12 rounded-lg bg-orange-100 dark:bg-[#28323e] text-orange-600 dark:text-white shrink-0">
<span class="material-symbols-outlined filled" style="font-variation-settings: 'FILL' 1;">sensors</span>
</div>
<div class="flex flex-col">
<h3 class="text-base font-semibold text-slate-900 dark:text-white leading-tight">Living Room</h3>
<p class="text-sm text-slate-500 dark:text-slate-400">Motion Sensor</p>
</div>
</div>
<div class="flex flex-col items-center justify-center min-w-[60px]">
<span class="material-symbols-outlined text-slate-400 dark:text-slate-500 mb-0.5" style="font-size: 24px; font-variation-settings: 'FILL' 1;">battery_full</span>
<span class="text-xs font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap tabular-nums">312 days</span>
</div>
</div>
<div class="group relative flex items-center justify-between p-4 bg-white dark:bg-[#1c242d] rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 transition-transform active:scale-[0.98]">
<div class="flex items-center gap-4">
<div class="flex items-center justify-center w-12 h-12 rounded-lg bg-purple-100 dark:bg-[#28323e] text-purple-600 dark:text-white shrink-0">
<span class="material-symbols-outlined filled" style="font-variation-settings: 'FILL' 1;">smart_button</span>
</div>
<div class="flex flex-col">
<h3 class="text-base font-semibold text-slate-900 dark:text-white leading-tight">Bedroom Lights</h3>
<p class="text-sm text-slate-500 dark:text-slate-400">Remote Button</p>
</div>
</div>
<div class="flex flex-col items-center justify-center min-w-[60px]">
<span class="material-symbols-outlined text-slate-400 dark:text-slate-500 mb-0.5" style="font-size: 24px; font-variation-settings: 'FILL' 1;">battery_full</span>
<span class="text-xs font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap tabular-nums">12 days</span>
</div>
</div>
<div class="group relative flex items-center justify-between p-4 bg-white dark:bg-[#1c242d] rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 transition-transform active:scale-[0.98]">
<div class="flex items-center gap-4">
<div class="flex items-center justify-center w-12 h-12 rounded-lg bg-red-100 dark:bg-[#28323e] text-red-600 dark:text-white shrink-0">
<span class="material-symbols-outlined filled" style="font-variation-settings: 'FILL' 1;">detector_smoke</span>
</div>
<div class="flex flex-col">
<h3 class="text-base font-semibold text-slate-900 dark:text-white leading-tight">Kitchen</h3>
<p class="text-sm text-slate-500 dark:text-slate-400">Smoke Detector</p>
</div>
</div>
<div class="flex flex-col items-center justify-center min-w-[60px]">
<span class="material-symbols-outlined text-red-500 dark:text-red-400 mb-0.5" style="font-size: 24px; font-variation-settings: 'FILL' 1;">battery_alert</span>
<span class="text-xs font-bold text-red-600 dark:text-red-400 whitespace-nowrap tabular-nums">412 days</span>
</div>
</div>
<div class="group relative flex items-center justify-between p-4 bg-white dark:bg-[#1c242d] rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 transition-transform active:scale-[0.98]">
<div class="flex items-center gap-4">
<div class="flex items-center justify-center w-12 h-12 rounded-lg bg-teal-100 dark:bg-[#28323e] text-teal-600 dark:text-white shrink-0">
<span class="material-symbols-outlined filled" style="font-variation-settings: 'FILL' 1;">thermostat</span>
</div>
<div class="flex flex-col">
<h3 class="text-base font-semibold text-slate-900 dark:text-white leading-tight">Hallway</h3>
<p class="text-sm text-slate-500 dark:text-slate-400">Thermostat</p>
</div>
</div>
<div class="flex flex-col items-center justify-center min-w-[60px]">
<span class="material-symbols-outlined text-slate-400 dark:text-slate-500 mb-0.5" style="font-size: 24px; font-variation-settings: 'FILL' 1;">battery_full</span>
<span class="text-xs font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap tabular-nums">89 days</span>
</div>
</div>
<div class="group relative flex items-center justify-between p-4 bg-white dark:bg-[#1c242d] rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 transition-transform active:scale-[0.98]">
<div class="flex items-center gap-4">
<div class="flex items-center justify-center w-12 h-12 rounded-lg bg-slate-100 dark:bg-[#28323e] text-slate-600 dark:text-white shrink-0">
<span class="material-symbols-outlined filled" style="font-variation-settings: 'FILL' 1;">garage_home</span>
</div>
<div class="flex flex-col">
<h3 class="text-base font-semibold text-slate-900 dark:text-white leading-tight">Garage</h3>
<p class="text-sm text-slate-500 dark:text-slate-400">Opener Keypad</p>
</div>
</div>
<div class="flex flex-col items-center justify-center min-w-[60px]">
<span class="material-symbols-outlined text-slate-400 dark:text-slate-500 mb-0.5" style="font-size: 24px; font-variation-settings: 'FILL' 1;">battery_full</span>
<span class="text-xs font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap tabular-nums">150 days</span>
</div>
</div>
</main>
<nav class="flex-none bg-white dark:bg-[#1c242d] border-t border-slate-200 dark:border-slate-800 pb-6 pt-2 px-6">
<div class="flex justify-between items-center max-w-md mx-auto">
<a class="flex flex-1 flex-col items-center justify-end gap-1 text-primary" href="#">
<span class="material-symbols-outlined filled" style="font-variation-settings: 'FILL' 1;">format_list_bulleted</span>
<span class="text-[10px] font-medium tracking-wide">Devices</span>
</a>
<a class="flex flex-1 flex-col items-center justify-end gap-1 text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 transition-colors" href="#">
<span class="material-symbols-outlined">history</span>
<span class="text-[10px] font-medium tracking-wide">History</span>
</a>
<a class="flex flex-1 flex-col items-center justify-end gap-1 text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 transition-colors" href="#">
<span class="material-symbols-outlined">settings</span>
<span class="text-[10px] font-medium tracking-wide">Settings</span>
</a>
</div>
</nav>

</body></html>