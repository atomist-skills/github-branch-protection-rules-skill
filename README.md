# `@atomist/github-branch-protection-rules`

<!---atomist-skill-readme:start--->

Github Issues allows custom branch protection rules for each Repository.  This skill allows teams to configure one standard set
of rules that will be applied across all Repositories. 

## Configuration

---

### Name

Give your configuration of this skill a distinctive name.  You might only need one configuration of this skill when
you use the same set of Issue Labels across all of your Orgs and Repos.  However, you can create distinct configurations
if you have different sets of Labels that you want to use for different sets of Repositories.

### Standard Labels

Configure a set of standard Labels.  The labels must be formatted:

```
label_name:long description of the label
```

The `label_name` and `description` of the label should be separated by a `:`.  The `label_name` itself can not contain a colon.

### Cron Schedule

This skill will periodically check that all labels are synchronized across your team's repositories.

### Which repositories

By default, this skill will be enabled for all repositories in all organizations you have connected.
To restrict the organizations or specific repositories on which the skill will run, you can explicitly
choose organization(s) and repositories.

## Integrations

---

**GitHub**

The Atomist GitHub integration must be configured to used this skill. At least one repository must be selected.

<!---atomist-skill-readme:end--->

---

Created by [Atomist][atomist].
Need Help?  [Join our Slack workspace][slack].

[atomist]: https://atomist.com/ (Atomist - How Teams Deliver Software)
[slack]: https://join.atomist.com/ (Atomist Community Slack) 
