data = read.table("results/anova_user_mean.txt", sep="\t", header = TRUE)
data$user = factor(data$user)
data$keyboard_type = factor(data$keyboard_type)

cat("\n========== Speed (WPM) ~ Keyboard ==========\n")
with(data, tapply(wpm, keyboard_type, mean))
with(data, tapply(wpm, keyboard_type, sd))
aov.out = aov(wpm ~ keyboard_type + Error(user/keyboard_type), data=data)
summary(aov.out)
with(data, pairwise.t.test(x=wpm, g=keyboard_type, p.adjust.method="holm", paired=T))

cat("\n========== Word Error Rate (%) ~ Keyboard ==========\n")
with(data, tapply(wer, keyboard_type, mean))
with(data, tapply(wer, keyboard_type, sd))
aov.out = aov(wer ~ keyboard_type + Error(user/keyboard_type), data=data)
summary(aov.out)
with(data, pairwise.t.test(x=wer, g=keyboard_type, p.adjust.method="holm", paired=T))
