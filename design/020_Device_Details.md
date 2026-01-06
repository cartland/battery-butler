A device details view (to be viewed when tapping a device from the device list). This should show the icon, name, type as the primary header. It should also show the battery type (AA, AAA, CR2032, etc. any string), and the number of batteries (integer). There is an "edit" option to change any of these fields.

Proposal in HTML
The history cards should be the same as the history in Battery_History.md. I prefer the design in Battery_History.md, except I prefer the Month + Day style from this design. The Month + Day should be on the right hand side of the card design from Battery_History.md.


<!DOCTYPE html>

<html class="dark" lang="en"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>Device Details View</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
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
                    borderRadius: {
                        "DEFAULT": "0.25rem",
                        "lg": "0.5rem",
                        "xl": "0.75rem",
                        "2xl": "1rem",
                        "full": "9999px"
                    },
                },
            },
        }
    </script>
<style>
        .material-symbols-outlined {
            font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
        }
    </style>
<style>
    body {
      min-height: max(884px, 100dvh);
    }
  </style>
  </head>
<body class="bg-background-light dark:bg-background-dark font-display text-gray-900 dark:text-white overflow-x-hidden min-h-screen">
<div class="relative flex h-full w-full flex-col max-w-md mx-auto min-h-screen shadow-lg dark:shadow-none bg-background-light dark:bg-background-dark">
<!-- Top App Bar -->
<div class="sticky top-0 z-50 flex items-center bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-sm p-4 justify-between border-b border-gray-200 dark:border-gray-800">
<button class="flex size-10 items-center justify-center text-primary hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors">
<span class="material-symbols-outlined text-[24px]">arrow_back_ios_new</span>
</button>
<h2 class="text-base dark:text-white font-bold leading-tight tracking-tight">Device Details</h2>
<button class="flex px-2 h-8 items-center justify-center rounded-lg text-primary text-base font-medium hover:bg-primary/10 transition-colors">
                Edit
            </button>
</div>
<div class="flex-1 overflow-y-auto pb-8">
<!-- Profile Header -->
<div class="flex flex-col items-center pt-8 pb-6 px-4">
<div class="relative mb-4 group cursor-pointer">
<div class="flex items-center justify-center w-28 h-28 rounded-full bg-gradient-to-br from-gray-200 to-gray-300 dark:from-gray-800 dark:to-gray-900 shadow-inner">
<!-- Placeholder for device image or icon -->
<span class="material-symbols-outlined text-6xl text-gray-500 dark:text-gray-400">videogame_asset</span>
</div>
<div class="absolute bottom-0 right-0 p-1.5 bg-primary rounded-full border-4 border-background-light dark:border-background-dark flex items-center justify-center shadow-sm">
<span class="material-symbols-outlined text-white text-sm">edit</span>
</div>
</div>
<h1 class="text-2xl font-bold text-gray-900 dark:text-white text-center mb-1">Xbox Controller</h1>
<div class="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
<span class="material-symbols-outlined text-sm">meeting_room</span>
<span class="text-sm font-medium">Living Room</span>
</div>
</div>
<!-- Stats Grid -->
<div class="grid grid-cols-2 gap-4 px-4 mb-6">
<div class="flex flex-col gap-1 rounded-xl p-4 bg-white dark:bg-[#1C252E] shadow-sm border border-gray-100 dark:border-gray-800">
<div class="flex items-center gap-2 mb-2">
<div class="p-1.5 rounded-md bg-primary/10 text-primary">
<span class="material-symbols-outlined text-xl">battery_charging_full</span>
</div>
<p class="text-gray-500 dark:text-gray-400 text-xs font-semibold uppercase tracking-wider">Type</p>
</div>
<p class="text-gray-900 dark:text-white text-xl font-bold">AA</p>
</div>
<div class="flex flex-col gap-1 rounded-xl p-4 bg-white dark:bg-[#1C252E] shadow-sm border border-gray-100 dark:border-gray-800">
<div class="flex items-center gap-2 mb-2">
<div class="p-1.5 rounded-md bg-primary/10 text-primary">
<span class="material-symbols-outlined text-xl">numbers</span>
</div>
<p class="text-gray-500 dark:text-gray-400 text-xs font-semibold uppercase tracking-wider">Quantity</p>
</div>
<p class="text-gray-900 dark:text-white text-xl font-bold">2</p>
</div>
</div>
<!-- Action Section -->
<div class="px-4 mb-8">
<button class="w-full flex items-center justify-between p-4 bg-primary text-white rounded-xl shadow-lg shadow-primary/25 active:scale-[0.98] transition-all duration-200">
<div class="flex items-center gap-3">
<div class="bg-white/20 p-2 rounded-lg backdrop-blur-sm">
<span class="material-symbols-outlined text-[24px]">add_circle</span>
</div>
<div class="flex flex-col items-start">
<span class="font-bold text-base">Record Replacement</span>
<span class="text-white/80 text-xs">Log battery change for today</span>
</div>
</div>
<span class="material-symbols-outlined">chevron_right</span>
</button>
</div>
<!-- History Section -->
<div class="flex flex-col gap-0 px-4">
<div class="flex items-center justify-between mb-3 px-1">
<h3 class="text-lg font-bold text-gray-900 dark:text-white">History</h3>
<button class="text-xs text-primary font-medium hover:underline">View All</button>
</div>
<div class="flex flex-col bg-white dark:bg-[#1C252E] rounded-xl border border-gray-100 dark:border-gray-800 overflow-hidden shadow-sm">
<!-- History Item 1 -->
<div class="flex items-center gap-4 p-4 border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors cursor-pointer group">
<div class="flex flex-col items-center justify-center h-12 w-12 rounded-lg bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 shrink-0 group-hover:text-primary group-hover:bg-primary/10 transition-colors">
<span class="text-xs font-bold uppercase">OCT</span>
<span class="text-lg font-bold leading-none">12</span>
</div>
<div class="flex flex-col flex-1 min-w-0">
<p class="text-gray-500 dark:text-gray-400 text-sm truncate">4 months ago</p>
</div>
<div class="shrink-0 text-gray-400">
<span class="material-symbols-outlined">chevron_right</span>
</div>
</div>
<!-- History Item 2 -->
<div class="flex items-center gap-4 p-4 border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors cursor-pointer group">
<div class="flex flex-col items-center justify-center h-12 w-12 rounded-lg bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 shrink-0 group-hover:text-primary group-hover:bg-primary/10 transition-colors">
<span class="text-xs font-bold uppercase">JAN</span>
<span class="text-lg font-bold leading-none">10</span>
</div>
<div class="flex flex-col flex-1 min-w-0">
<p class="text-gray-500 dark:text-gray-400 text-sm truncate">1 year ago</p>
</div>
<div class="shrink-0 text-gray-400">
<span class="material-symbols-outlined">chevron_right</span>
</div>
</div>
<!-- History Item 3 -->
<div class="flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors cursor-pointer group">
<div class="flex flex-col items-center justify-center h-12 w-12 rounded-lg bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 shrink-0 group-hover:text-primary group-hover:bg-primary/10 transition-colors">
<span class="text-xs font-bold uppercase">SEP</span>
<span class="text-lg font-bold leading-none">05</span>
</div>
<div class="flex flex-col flex-1 min-w-0">
<p class="text-gray-500 dark:text-gray-400 text-sm truncate">1 year ago</p>
</div>
<div class="shrink-0 text-gray-400">
<span class="material-symbols-outlined">chevron_right</span>
</div>
</div>
</div>
</div>
<div class="h-10"></div>
</div>
</div>
</body></html>