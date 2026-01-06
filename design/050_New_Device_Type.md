Create a screen for the "New Device Type". This has a Name, Battery Type, Battery Quantity, and Icon.

Proposed HTML


<!DOCTYPE html>

<html class="dark" lang="en"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>Add Device Type</title>
<!-- Google Fonts: Inter and Material Symbols -->
<link href="https://fonts.googleapis.com" rel="preconnect"/>
<link crossorigin="" href="https://fonts.gstatic.com" rel="preconnect"/>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<!-- Tailwind CSS -->
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<!-- Tailwind Configuration -->
<script id="tailwind-config">
        tailwind.config = {
            darkMode: "class",
            theme: {
                extend: {
                    colors: {
                        "primary": "#137fec",
                        "background-light": "#f6f7f8",
                        "background-dark": "#101922",
                        "surface-dark": "#1c242e", // Slightly lighter than background-dark for cards/inputs
                    },
                    fontFamily: {
                        "display": ["Inter", "sans-serif"]
                    },
                    borderRadius: {
                        "DEFAULT": "0.25rem", 
                        "lg": "0.5rem", 
                        "xl": "0.75rem", 
                        "full": "9999px"
                    },
                },
            },
        }
    </script>
<style>
        /* Custom scrollbar hiding for cleaner mobile look */
        .no-scrollbar::-webkit-scrollbar {
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
  </head>
<body class="font-display bg-background-light dark:bg-background-dark text-[#111418] dark:text-white overflow-hidden">
<div class="relative flex h-full min-h-screen w-full flex-col group/design-root overflow-x-hidden">
<!-- Top Navigation Bar -->
<div class="flex items-center px-4 py-3 justify-between border-b border-gray-200 dark:border-gray-800 bg-background-light dark:bg-background-dark sticky top-0 z-10">
<div class="flex w-16 items-center justify-start">
<button class="text-gray-500 dark:text-[#9dabb9] text-base font-medium hover:text-gray-900 dark:hover:text-white transition-colors">
                    Cancel
                </button>
</div>
<h2 class="text-[#111418] dark:text-white text-lg font-bold leading-tight tracking-[-0.015em] text-center flex-1">New Device Type</h2>
<div class="flex w-16 items-center justify-end">
<button class="text-primary text-base font-bold leading-normal tracking-[0.015em] shrink-0 hover:opacity-80 transition-opacity">
                    Save
                </button>
</div>
</div>
<!-- Scrollable Content -->
<div class="flex-1 overflow-y-auto no-scrollbar pb-10">
<!-- Icon Selector Section -->
<div class="pt-6 pb-2">
<h3 class="text-[#111418] dark:text-white tracking-tight text-xl font-bold leading-tight px-4 text-left pb-4">Choose an Icon</h3>
<!-- Icon Grid -->
<div class="grid grid-cols-4 gap-4 px-4">
<!-- Selected Icon -->
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-primary text-white border-2 border-primary shadow-[0_0_15px_rgba(19,127,236,0.3)] transition-all">
<span class="material-symbols-outlined text-3xl">videogame_asset</span>
</div>
<span class="text-xs font-medium text-primary">Game</span>
</div>
<!-- Unselected Icons -->
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-gray-200 dark:bg-surface-dark text-gray-500 dark:text-gray-400 border-2 border-transparent group-hover:border-gray-300 dark:group-hover:border-gray-600 transition-all">
<span class="material-symbols-outlined text-3xl">tv</span>
</div>
<span class="text-xs font-medium text-gray-500 dark:text-gray-400">Remote</span>
</div>
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-gray-200 dark:bg-surface-dark text-gray-500 dark:text-gray-400 border-2 border-transparent group-hover:border-gray-300 dark:group-hover:border-gray-600 transition-all">
<span class="material-symbols-outlined text-3xl">toys</span>
</div>
<span class="text-xs font-medium text-gray-500 dark:text-gray-400">Toy</span>
</div>
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-gray-200 dark:bg-surface-dark text-gray-500 dark:text-gray-400 border-2 border-transparent group-hover:border-gray-300 dark:group-hover:border-gray-600 transition-all">
<span class="material-symbols-outlined text-3xl">mouse</span>
</div>
<span class="text-xs font-medium text-gray-500 dark:text-gray-400">Mouse</span>
</div>
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-gray-200 dark:bg-surface-dark text-gray-500 dark:text-gray-400 border-2 border-transparent group-hover:border-gray-300 dark:group-hover:border-gray-600 transition-all">
<span class="material-symbols-outlined text-3xl">keyboard</span>
</div>
<span class="text-xs font-medium text-gray-500 dark:text-gray-400">Keyboard</span>
</div>
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-gray-200 dark:bg-surface-dark text-gray-500 dark:text-gray-400 border-2 border-transparent group-hover:border-gray-300 dark:group-hover:border-gray-600 transition-all">
<span class="material-symbols-outlined text-3xl">detector_smoke</span>
</div>
<span class="text-xs font-medium text-gray-500 dark:text-gray-400">Sensor</span>
</div>
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-gray-200 dark:bg-surface-dark text-gray-500 dark:text-gray-400 border-2 border-transparent group-hover:border-gray-300 dark:group-hover:border-gray-600 transition-all">
<span class="material-symbols-outlined text-3xl">flashlight_on</span>
</div>
<span class="text-xs font-medium text-gray-500 dark:text-gray-400">Light</span>
</div>
<div class="flex flex-col gap-2 items-center cursor-pointer group">
<div class="w-16 h-16 rounded-full flex items-center justify-center bg-gray-200 dark:bg-surface-dark text-gray-500 dark:text-gray-400 border-2 border-transparent group-hover:border-gray-300 dark:group-hover:border-gray-600 transition-all">
<span class="material-symbols-outlined text-3xl">schedule</span>
</div>
<span class="text-xs font-medium text-gray-500 dark:text-gray-400">Clock</span>
</div>
</div>
</div>
<!-- Divider -->
<div class="h-px bg-gray-200 dark:bg-gray-800 mx-4 my-6"></div>
<!-- Form Section -->
<div class="px-4">
<h3 class="text-[#111418] dark:text-white tracking-tight text-xl font-bold leading-tight text-left pb-4">Device Details</h3>
<div class="flex flex-col gap-5">
<!-- Name Input -->
<label class="flex flex-col w-full">
<p class="text-[#111418] dark:text-white text-base font-medium leading-normal pb-2">Name</p>
<input class="form-input flex w-full min-w-0 resize-none overflow-hidden rounded-xl text-[#111418] dark:text-white focus:outline-0 focus:ring-2 focus:ring-primary/50 border border-gray-300 dark:border-[#3b4754] bg-white dark:bg-surface-dark h-14 placeholder:text-gray-400 dark:placeholder:text-[#9dabb9] px-4 text-base font-normal leading-normal transition-all" placeholder="e.g. Xbox Controller" value=""/>
</label>
<!-- Battery Type Selector -->
<div class="flex flex-col w-full">
<p class="text-[#111418] dark:text-white text-base font-medium leading-normal pb-2">Battery Type</p>
<div class="relative">
<select class="form-select w-full appearance-none rounded-xl border border-gray-300 dark:border-[#3b4754] bg-white dark:bg-surface-dark text-[#111418] dark:text-white h-14 px-4 pr-10 text-base font-normal focus:border-primary focus:ring-2 focus:ring-primary/50 transition-all">
<option>AA</option>
<option>AAA</option>
<option>CR2032</option>
<option>9V</option>
<option>D</option>
<option>C</option>
<option>Li-ion Custom</option>
</select>
<div class="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-gray-500 dark:text-[#9dabb9]">
<span class="material-symbols-outlined">expand_more</span>
</div>
</div>
</div>
<!-- Quantity Stepper -->
<div class="flex flex-col w-full">
<p class="text-[#111418] dark:text-white text-base font-medium leading-normal pb-2">Battery Quantity</p>
<div class="flex items-center justify-between p-2 pr-4 rounded-xl border border-gray-300 dark:border-[#3b4754] bg-white dark:bg-surface-dark h-16">
<div class="flex items-center gap-3">
<div class="w-10 h-10 flex items-center justify-center bg-gray-100 dark:bg-gray-800 rounded-lg ml-2">
<span class="material-symbols-outlined text-gray-500 dark:text-gray-400">battery_horiz_075</span>
</div>
<span class="text-sm text-gray-500 dark:text-gray-400">Batteries needed</span>
</div>
<div class="flex items-center gap-4">
<button class="w-10 h-10 flex items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-700 text-[#111418] dark:text-white hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">
<span class="material-symbols-outlined text-xl">remove</span>
</button>
<span class="text-xl font-bold text-[#111418] dark:text-white w-4 text-center">2</span>
<button class="w-10 h-10 flex items-center justify-center rounded-lg bg-primary text-white hover:bg-blue-600 transition-colors shadow-lg shadow-blue-500/30">
<span class="material-symbols-outlined text-xl">add</span>
</button>
</div>
</div>
</div>
</div>
</div>
<!-- Helper Text Footer -->
<div class="mt-8 px-6 text-center">
<p class="text-sm text-gray-500 dark:text-[#9dabb9] leading-relaxed">
                    This creates a template for future devices. You can add specific tracking details later.
                </p>
</div>
<div class="h-20"></div> <!-- Spacer for scrolling -->
</div>
</div>
</body></html>

