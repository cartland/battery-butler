Create a "Device Types" screen that shows the list of Device Types, including their names, battery type, and battery quantity.


PRoposed HTML


<!DOCTYPE html>

<html class="dark" lang="en"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>Device Types List</title>
<!-- Fonts -->
<link href="https://fonts.googleapis.com" rel="preconnect"/>
<link crossorigin="" href="https://fonts.gstatic.com" rel="preconnect"/>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<!-- Tailwind -->
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
                "card-dark": "#18232c", 
                "card-lighter": "#202d38"
                },
                fontFamily: {
                "display": ["Inter", "sans-serif"]
                },
                borderRadius: {"DEFAULT": "0.25rem", "lg": "0.5rem", "xl": "0.75rem", "2xl": "1rem", "full": "9999px"},
            },
            },
        }
    </script>
<style>
        /* Custom scrollbar for webkit */
        ::-webkit-scrollbar {
            width: 6px;
        }
        ::-webkit-scrollbar-track {
            background: #101922;
        }
        ::-webkit-scrollbar-thumb {
            background: #202d38;
            border-radius: 3px;
        }
        ::-webkit-scrollbar-thumb:hover {
            background: #304150;
        }
    </style>
<style>
    body {
      min-height: max(884px, 100dvh);
    }
  </style>
  </head>
<body class="bg-background-light dark:bg-background-dark font-display text-gray-900 dark:text-white antialiased overflow-x-hidden">
<div class="relative flex h-full min-h-screen w-full flex-col max-w-md mx-auto shadow-2xl overflow-hidden">
<!-- TopAppBar -->
<header class="sticky top-0 z-20 bg-background-light dark:bg-background-dark/95 backdrop-blur-sm border-b border-gray-200 dark:border-white/5 pt-safe-top">
<div class="flex items-center justify-between p-4 pb-2">
<h2 class="text-2xl font-bold tracking-tight text-gray-900 dark:text-white flex-1">Device Types</h2>
<button aria-label="Add Device" class="group flex items-center justify-center rounded-full p-2 text-primary hover:bg-primary/10 transition-colors">
<span class="material-symbols-outlined text-[28px] font-medium">add_circle</span>
</button>
</div>
<!-- SearchBar -->
<div class="px-4 pb-4 pt-1">
<div class="group relative flex w-full items-center rounded-xl bg-white dark:bg-card-dark border border-gray-200 dark:border-transparent focus-within:border-primary/50 focus-within:ring-2 focus-within:ring-primary/20 transition-all shadow-sm">
<div class="flex h-11 w-11 items-center justify-center text-gray-400 dark:text-gray-500">
<span class="material-symbols-outlined text-[22px]">search</span>
</div>
<input class="h-full w-full bg-transparent border-none text-base text-gray-900 dark:text-white placeholder:text-gray-400 dark:placeholder:text-gray-500 focus:ring-0 pl-0 pr-4 rounded-xl" placeholder="Search devices..." type="text"/>
</div>
</div>
</header>
<!-- Scrollable List Content -->
<main class="flex-1 px-4 py-2 space-y-3 pb-24">
<!-- Section Label (Optional context) -->
<div class="flex items-center justify-between pt-2 pb-1 px-1">
<span class="text-xs font-semibold text-gray-500 uppercase tracking-wider">All Devices</span>
<span class="text-xs text-gray-500">4 items</span>
</div>
<!-- ListItem: TV Remote -->
<div class="group relative flex items-center gap-4 p-3 bg-white dark:bg-card-dark rounded-xl shadow-sm border border-gray-200 dark:border-white/5 active:scale-[0.98] transition-transform duration-200 cursor-pointer overflow-hidden">
<!-- Icon Box -->
<div class="flex shrink-0 items-center justify-center size-12 rounded-lg bg-primary/10 text-primary dark:bg-card-lighter dark:text-primary">
<span class="material-symbols-outlined text-[24px]">settings_remote</span>
</div>
<!-- Content -->
<div class="flex flex-1 flex-col justify-center gap-1 min-w-0">
<p class="text-base font-semibold text-gray-900 dark:text-gray-100 truncate">TV Remote</p>
<div class="flex items-center gap-2">
<span class="inline-flex items-center rounded-md bg-gray-100 dark:bg-white/10 px-2 py-0.5 text-xs font-medium text-gray-600 dark:text-gray-300 ring-1 ring-inset ring-gray-500/10 dark:ring-white/10">
                            AA
                        </span>
<span class="text-xs text-gray-500 dark:text-gray-400">Replaced 2m ago</span>
</div>
</div>
<!-- Qty -->
<div class="flex flex-col items-end justify-center shrink-0 pl-2">
<span class="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase mb-0.5">Qty</span>
<span class="text-lg font-bold text-gray-900 dark:text-white tabular-nums">2</span>
</div>
<!-- Hover Gradient Effect -->
<div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-white/10 opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity"></div>
</div>
<!-- ListItem: Kitchen Scale -->
<div class="group relative flex items-center gap-4 p-3 bg-white dark:bg-card-dark rounded-xl shadow-sm border border-gray-200 dark:border-white/5 active:scale-[0.98] transition-transform duration-200 cursor-pointer overflow-hidden">
<div class="flex shrink-0 items-center justify-center size-12 rounded-lg bg-primary/10 text-primary dark:bg-card-lighter dark:text-primary">
<span class="material-symbols-outlined text-[24px]">scale</span>
</div>
<div class="flex flex-1 flex-col justify-center gap-1 min-w-0">
<p class="text-base font-semibold text-gray-900 dark:text-gray-100 truncate">Kitchen Scale</p>
<div class="flex items-center gap-2">
<span class="inline-flex items-center rounded-md bg-blue-50 dark:bg-primary/20 px-2 py-0.5 text-xs font-medium text-blue-700 dark:text-blue-200 ring-1 ring-inset ring-blue-700/10">
                            CR2032
                        </span>
</div>
</div>
<div class="flex flex-col items-end justify-center shrink-0 pl-2">
<span class="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase mb-0.5">Qty</span>
<span class="text-lg font-bold text-gray-900 dark:text-white tabular-nums">1</span>
</div>
<div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-white/10 opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity"></div>
</div>
<!-- ListItem: Wall Clock -->
<div class="group relative flex items-center gap-4 p-3 bg-white dark:bg-card-dark rounded-xl shadow-sm border border-gray-200 dark:border-white/5 active:scale-[0.98] transition-transform duration-200 cursor-pointer overflow-hidden">
<div class="flex shrink-0 items-center justify-center size-12 rounded-lg bg-primary/10 text-primary dark:bg-card-lighter dark:text-primary">
<span class="material-symbols-outlined text-[24px]">schedule</span>
</div>
<div class="flex flex-1 flex-col justify-center gap-1 min-w-0">
<p class="text-base font-semibold text-gray-900 dark:text-gray-100 truncate">Wall Clock</p>
<div class="flex items-center gap-2">
<span class="inline-flex items-center rounded-md bg-gray-100 dark:bg-white/10 px-2 py-0.5 text-xs font-medium text-gray-600 dark:text-gray-300 ring-1 ring-inset ring-gray-500/10 dark:ring-white/10">
                            AA
                        </span>
</div>
</div>
<div class="flex flex-col items-end justify-center shrink-0 pl-2">
<span class="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase mb-0.5">Qty</span>
<span class="text-lg font-bold text-gray-900 dark:text-white tabular-nums">1</span>
</div>
<div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-white/10 opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity"></div>
</div>
<!-- ListItem: Flashlight -->
<div class="group relative flex items-center gap-4 p-3 bg-white dark:bg-card-dark rounded-xl shadow-sm border border-gray-200 dark:border-white/5 active:scale-[0.98] transition-transform duration-200 cursor-pointer overflow-hidden">
<div class="flex shrink-0 items-center justify-center size-12 rounded-lg bg-primary/10 text-primary dark:bg-card-lighter dark:text-primary">
<span class="material-symbols-outlined text-[24px]">flashlight_on</span>
</div>
<div class="flex flex-1 flex-col justify-center gap-1 min-w-0">
<p class="text-base font-semibold text-gray-900 dark:text-gray-100 truncate">Tactical Flashlight</p>
<div class="flex items-center gap-2">
<span class="inline-flex items-center rounded-md bg-orange-50 dark:bg-orange-500/20 px-2 py-0.5 text-xs font-medium text-orange-700 dark:text-orange-200 ring-1 ring-inset ring-orange-600/20">
                            D
                        </span>
<span class="text-xs text-orange-600 dark:text-orange-400/80 font-medium">Low Stock</span>
</div>
</div>
<div class="flex flex-col items-end justify-center shrink-0 pl-2">
<span class="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase mb-0.5">Qty</span>
<span class="text-lg font-bold text-gray-900 dark:text-white tabular-nums">4</span>
</div>
<div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-white/10 opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity"></div>
</div>
<!-- ListItem: Smoke Detector -->
<div class="group relative flex items-center gap-4 p-3 bg-white dark:bg-card-dark rounded-xl shadow-sm border border-gray-200 dark:border-white/5 active:scale-[0.98] transition-transform duration-200 cursor-pointer overflow-hidden">
<div class="flex shrink-0 items-center justify-center size-12 rounded-lg bg-primary/10 text-primary dark:bg-card-lighter dark:text-primary">
<span class="material-symbols-outlined text-[24px]">detector_smoke</span>
</div>
<div class="flex flex-1 flex-col justify-center gap-1 min-w-0">
<p class="text-base font-semibold text-gray-900 dark:text-gray-100 truncate">Hallway Detector</p>
<div class="flex items-center gap-2">
<span class="inline-flex items-center rounded-md bg-purple-50 dark:bg-purple-500/20 px-2 py-0.5 text-xs font-medium text-purple-700 dark:text-purple-200 ring-1 ring-inset ring-purple-700/10">
                            9V
                        </span>
</div>
</div>
<div class="flex flex-col items-end justify-center shrink-0 pl-2">
<span class="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase mb-0.5">Qty</span>
<span class="text-lg font-bold text-gray-900 dark:text-white tabular-nums">1</span>
</div>
<div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-white/10 opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity"></div>
</div>
</main>
<!-- Floating Action Button (Optional Alternative to Top Nav) / Empty Bottom Space -->
<!-- In iOS design, we keep the bottom clear for home indicator, but a gradient fade can be nice -->
<div class="pointer-events-none fixed bottom-0 left-0 right-0 h-12 bg-gradient-to-t from-background-light dark:from-background-dark to-transparent"></div>
</div>
<!-- Background texture/noise for subtle detail (Optional) -->
<div class="fixed inset-0 pointer-events-none opacity-[0.03] z-50 mix-blend-overlay" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuDxSu6eZ-VySAB5TzF8LrdyibcIcW5BH2uYWvwax3pNuygdm1cBFCcrKPIpF-K8UYecuP0a875N4s_SLvWz-Usu0Q50dLqMRzhnYsVOOMjEDq3heK6CMAcgCcQogntRYcvxpDsvamry-5i7JoHDBPSMZIxWnNziFtsy79G3tZnjkMnPEIbNOY5v4LOtY-Ah_ibPl6n5lXkqB9gXFXSqG4y6iqn_XAQfuvovk1SvczoT8COMlxgelXBYyYxE2u5zfKwBpBsOIY1wJ5A');"></div>
</body></html>