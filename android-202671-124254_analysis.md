# Perfetto Trace Analysis — android-202671-124254.pftrace
## Chain of Evidence (verified facts only)
- [T0] Trace file: /Users/arfinhossin/Downloads/Nekko/android-202671-124254.pftrace (32.66 MB)
- [T1] android_startups: NO app startup captured (empty result)
- [T2] Target app: package `app.usefoster`, upid=1, pid=16614 (identified from process table)
- [T4] Trace window: 770775015ms → 770784855ms = ~9.84s
- [T5] NO FrameTimeline data, NO app atrace slices (only binder slices in app). Jank analyzed via sched/thread_state.
- [T6] Main thread (utid=1) over 9.84s: Running 3481ms (35%), Sleeping 5745ms, R (runnable/CPU contention) 28.9ms, D+DK (uninterruptible I/O) 84.9ms / 83 segments
- [T7] Main thread: 102 Running bursts >8ms (total 1804ms); 15 bursts >16ms; worst 99.97ms @770780979 and 98.98ms @770782215 (≈6 dropped frames each @60Hz)
- [T8] RenderThread (utid=5): Running 1579ms, worst burst 44.34ms @770782171 (right before main-thread 98.98ms burst), D-state 32ms/191 segs, DK 13ms
- [T9] Memory pressure: kswapd0 ran 901ms total across 210 activations in 9.84s
- [T10] kswapd0 active during main-thread worst burst: activations @770782212-770782227 (1.8+0.05+2.1+4.5+5.2+4.1ms) directly precede/overlap the 98.98ms main-thread run
- [T11] App `app.usefoster` memory: rss.anon 154→167MB, rss.file 45.8→58.6MB, **mem.swap 71.5–76.5MB (76MB of app pages swapped out)**
- [T12] Main-thread DK stalls: worst 15.45ms @770782059, 15.42ms @770782330 (uninterruptible I/O = swap-in/page faults)
- [T13] kworker D-state total: 1171ms across 1774 segments (kernel I/O pressure system-wide)
- [T14] No CPU throttling: big cores at 2553–2803MHz during worst burst (max 2803MHz)
- [T15] CPU system-wide: system_server 11.31s, app.usefoster 5.58s, net.omobio.airtelsc 1.96s, kswapd0 0.90s
- [T16] Main thread R-state 28.9ms (max 5.2ms) → negligible CPU contention for main thread
